import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

class MySyncApiImplTest {

    private static final int LONG_OPERATION_DELAY = 1000;

    private static MySyncApiImpl prepareImplementation(boolean isSuccess) {
        MyAsyncApi apiMock = Mockito.mock(MyAsyncApi.class);

        Mockito.when(apiMock.operation(anyInt(), any(), any())).thenAnswer((Answer<Cancellable>) invocationOnMock -> {
            int param = invocationOnMock.getArgument(0);
            Callback callback = invocationOnMock.getArgument(1);
            ErrorCallback errorCallback = invocationOnMock.getArgument(2);
            new Thread(() -> {
                try {
                    Thread.sleep(LONG_OPERATION_DELAY);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (isSuccess) {
                    callback.onSuccess(param * 10);
                } else {
                    try {
                        errorCallback.onError(new MyApiException());
                    } catch (MyApiException ignore) {
                    }
                }
            }).start();
            return () -> {
                try {
                    Thread.sleep(LONG_OPERATION_DELAY);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Canceled");
            };
        });
        return new MySyncApiImpl(apiMock);
    }

    @org.junit.jupiter.api.Test
    void asyncOperation() {
        MySyncApiImpl mySyncApiImpl = prepareImplementation(true);
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call without must be without Exception
        Thread theFirstCall = new Thread(() -> assertDoesNotThrow(() -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must throw Exception
        Thread theSecondCall = new Thread(() -> assertThrows(IllegalStateException.class, () -> mySyncApiImpl.operation(2)));
        threads.add(theSecondCall);

        theFirstCall.start();
        // Sleep to be sure that theFirstCall called first
        try {
            Thread.sleep(LONG_OPERATION_DELAY / 4);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        theSecondCall.start();

        // Wait while all threads finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void asyncOperationWithError() {
        MySyncApiImpl mySyncApiImpl = prepareImplementation(false);
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call without must be without Exception
        Thread theFirstCall = new Thread(() -> assertThrows(MyApiException.class, () -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must throw Exception
        Thread theSecondCall = new Thread(() -> assertThrows(IllegalStateException.class, () -> mySyncApiImpl.operation(2)));
        threads.add(theSecondCall);

        theFirstCall.start();
        // Sleep to be sure that theFirstCall called first
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        theSecondCall.start();

        // Wait while all threads finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void syncOperation() {
        MySyncApiImpl mySyncApiImpl = prepareImplementation(true);
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call must be without Exception
        Thread theFirstCall = new Thread(() -> assertDoesNotThrow(() -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must be without Exception too
        Thread theSecondCall = new Thread(() -> assertDoesNotThrow(() -> mySyncApiImpl.operation(2)));
        threads.add(theSecondCall);

        theFirstCall.start();
        // Sleep to be sure that theFirstCall has been done
        try {
            Thread.sleep(LONG_OPERATION_DELAY * 2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        theSecondCall.start();

        // Wait while all threads finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void cancelOperation() {
        MySyncApiImpl mySyncApiImpl = prepareImplementation(true);
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call must be without Exception
        Thread theFirstCall = new Thread(() -> assertThrows(CancellationException.class, () -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        AtomicInteger trues = new AtomicInteger(0);
        Thread theZeroCancelCall = new Thread(() -> {
            if (mySyncApiImpl.cancelOperation()) trues.incrementAndGet();
        });
        threads.add(theZeroCancelCall);

        Thread theFirstCancelCall = new Thread(() -> {
            if (mySyncApiImpl.cancelOperation()) trues.incrementAndGet();
        });
        threads.add(theFirstCancelCall);

        Thread theSecondCancelCall = new Thread(() -> {
            if (mySyncApiImpl.cancelOperation()) trues.incrementAndGet();
        });
        threads.add(theSecondCancelCall);

        theZeroCancelCall.start();

        theFirstCall.start();
        // Sleep to be sure that theFirstCall called first
        try {
            Thread.sleep(LONG_OPERATION_DELAY / 4);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        theFirstCancelCall.start();
        theSecondCancelCall.start();

        // Wait while all threads finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        assertEquals(1, trues.get());
    }
}