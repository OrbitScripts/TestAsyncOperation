import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

class MySyncApiImplTest {

    private static final int LONG_OPERATION_DELAY = 2000;

    private static MySyncApiImpl prepareImplementation() {
        MyAsyncApi apiMock = Mockito.mock(MyAsyncApi.class);
        Mockito.when(apiMock.operation(anyInt(), any(), any()))
                .thenAnswer((Answer<Cancellable>) invocationOnMock -> {
                    int param = invocationOnMock.getArgument(0);
                    Callback callback = invocationOnMock.getArgument(1);
                    new Thread(() -> {
                        try {
                            Thread.sleep(LONG_OPERATION_DELAY);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        callback.onSuccess(param * 10);
                    }).start();
                    return () -> System.out.println("Canceled");
                });
        return new MySyncApiImpl(apiMock);
    }

    @org.junit.jupiter.api.Test
    void asyncOperation() {
        MySyncApiImpl mySyncApiImpl = prepareImplementation();
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call without must be without Exception
        Thread theFirstCall = new Thread(() -> assertDoesNotThrow(() -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must throw Exception
        Thread theSecondCall = new Thread(() ->
                assertThrows(IllegalStateException.class, () -> mySyncApiImpl.operation(2)));
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
        MySyncApiImpl mySyncApiImpl = prepareImplementation();
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call must be without Exception
        Thread theFirstCall = new Thread(() -> assertDoesNotThrow(() -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must be without Exception too
        Thread theSecondCall = new Thread(() ->
                assertDoesNotThrow(() -> mySyncApiImpl.operation(2)));
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
        MySyncApiImpl mySyncApiImpl = prepareImplementation();
        ArrayList<Thread> threads = new ArrayList<>();

        // The first call must be without Exception
        Thread theFirstCall = new Thread(() -> assertThrows(CancellationException.class, () -> mySyncApiImpl.operation(1)));
        threads.add(theFirstCall);

        // The second call must be without Exception too
        Thread theSecondCall = new Thread(() ->
                assertDoesNotThrow(() -> mySyncApiImpl.operation(2)));
        threads.add(theSecondCall);

        theFirstCall.start();
        // Sleep to be sure that theFirstCall has been done
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mySyncApiImpl.cancelOperation();
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
}