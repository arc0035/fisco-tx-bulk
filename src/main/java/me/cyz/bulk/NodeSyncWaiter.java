package me.cyz.bulk;

import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.client.Client;

import java.util.concurrent.*;

public class NodeSyncWaiter implements Future<Boolean> {

    public static void main(String[] args) throws Exception{
        new NodeSyncWaiter(5070).isDone();
    }

    private long targetBlock;

    public NodeSyncWaiter(long targetBlock){
        this.targetBlock = targetBlock;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        BcosSDK bcosSDK = BcosSDK.build("conf/config.toml");
        Client client = bcosSDK.getClient(1);
        long begin = System.currentTimeMillis();
        long current;
        System.out.println("Begin querying...");
        while((current = client.getBlockNumber().getBlockNumber().longValue()) < targetBlock){
            System.out.println("Current number: "+ current);
            try{
                Thread.sleep(20000);
            }
            catch (Exception ex){}
        }

        long end = System.currentTimeMillis();
        System.out.println("Time elapsed :" + (end - begin)/1000 + " seconds");
        return true;
    }

    @Override
    public Boolean get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
