package bq_standard.handlers;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ServerTaskExecutor implements ITickHandler {
    private static final ArrayDeque<FutureTask> serverTasks = new ArrayDeque<FutureTask>();

    @Override
    public void tickStart(EnumSet<TickType> enumSet, Object... objects) {

        synchronized(serverTasks)
        {
            while(!serverTasks.isEmpty()) serverTasks.poll().run();
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> enumSet, Object... objects) {

    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.SERVER);
    }

    @Override
    public String getLabel() {
        return "BQS_ServerTaskExecutor";
    }

    // NOTE: This is slightly different to the version in the base mod.
    // This one will not immediately run tasks even if it's from the same thread.
    public static <T> ListenableFuture<T> scheduleServerTask(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException("task cannot be null");
        }

        ListenableFutureTask<T> listenablefuturetask = ListenableFutureTask.create(task);

        synchronized (serverTasks) {
            serverTasks.add(listenablefuturetask);
            return listenablefuturetask;
        }
    }
}
