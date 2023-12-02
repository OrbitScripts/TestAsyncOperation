//Given below definitions implement MySyncApiImpl.operation and MySyncApiImpl.cancelOperation methods. 
//Send me a link to github repo or a gist with your implementation.

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

public class MySyncApiImpl {
    private final MyAsyncApi api;
    private final Semaphore semaphore = new Semaphore(1);

    private CompletableFuture<Integer> future;

    public MySyncApiImpl(MyAsyncApi api) {
        this.api = api;
    }

    /**
     * Runs MyAsyncApi.operation and blocks until it finishes. Throws IllegalStateException if called while operation is running.
     */
    public int operation(int param) throws MyApiException, IllegalStateException {
        if (semaphore.tryAcquire()) {
            future = new CompletableFuture<>();
            Cancellable cancellable = api.operation(param, future::complete, future::completeExceptionally);
            try {
                future.join();
                if (future.isDone()) {
                    return future.get();
                } else if (future.isCompletedExceptionally()) {
                    throw future.exceptionNow();
                } else {
                    throw new IllegalStateException();
                }
            } catch (CancellationException e) {
                cancellable.cancel();
                throw e;
            } catch (MyApiException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                future = null;
                semaphore.release();
            }

        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Cancel most recent operation started with 'operation' method.
     * Return true of operation was cancelled and false otherwise.
     */
    public boolean cancelOperation() {
        if (future != null) {
            future.cancel(true);
            return true;
        }
        return false;
    }


}

