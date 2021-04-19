package me.cyz.bulk;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.abi.datatypes.Int;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.crypto.keypair.ECDSAKeyPair;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignature;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.transaction.builder.TransactionBuilderService;
import org.fisco.bcos.sdk.transaction.codec.encode.TransactionEncoderService;
import org.fisco.bcos.sdk.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.transaction.model.po.RawTransaction;
import org.fisco.bcos.sdk.transaction.pusher.TransactionPusherService;
import org.fisco.bcos.sdk.transaction.signer.TransactionSignerServcie;

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author aaronchu
 * @Description
 * @data 2021/03/30
 */
public class App {
    //ABI和BIN可通过控制台脚本，或gradle编译插件等方式获取
    public static final String ABI = IOUtil.readResourceAsString("abi/HelloWorld.abi");
    public static final String BINARY = IOUtil.readResourceAsString("bin/ecc/HelloWorld.bin");

    private static void pushTransactionsV1(long txCount) throws Exception {
        BcosSDK sdk = BcosSDK.build("conf/config.toml");
        Client client = sdk.getClient(1);
        AssembleTransactionProcessor txProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                client,
                client.getCryptoSuite().createKeyPair());
        //Deploy
        TransactionResponse tr = txProcessor.deployAndGetResponse(ABI, BINARY, Arrays.asList());
        String address  = tr.getContractAddress();
        //Push transaction!
        for (int i = 0; i < txCount; i++) {
            txProcessor.sendTransactionAndGetResponse(address, ABI, "set", Arrays.asList("test"));
        }
    }


    public static void main(String[] args) throws Throwable{
        long begin = System.currentTimeMillis();
        pushTransactionV3(100000,100);
        long end = System.currentTimeMillis();
        System.out.println("交易量："+100000);
        System.out.println("耗时(ms)："+(end - begin));
        /*
        final int txCount = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);
        final String method = args[2];

        if(method.equals("rpc")){
            insertByJsonRpc(txCount, threads);
        }
        else{
            insertByChannel(txCount, threads);
        }

         */
    }
    private static void pushTransactionV2(int txCount, int threadCount) throws Throwable {
        BcosSDK sdk =  BcosSDK.build("conf/config.toml");
        Client client = sdk.getClient(1);
        AssembleTransactionProcessor txProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                client,
                client.getCryptoSuite().createKeyPair());
        //Deploy
        TransactionResponse tr = txProcessor.deployAndGetResponse(ABI, BINARY, Arrays.asList());
        String address  = tr.getContractAddress();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        CountDownLatch latch = new CountDownLatch(txCount);
        for(int i=0;i<txCount;i++){
            executor.submit(() -> {
                try{
                    txProcessor.sendTransactionAndGetResponse(address, ABI, "set", Arrays.asList("name"));
                }
                catch (Exception ex){}
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }


    private static void pushTransactionV3(int txCount, int threadCount) throws Throwable{
        JsonRpcHttpClient jsonClient = new JsonRpcHttpClient(new URL("http://127.0.0.1:8545/"));
        BcosSDK sdk =  BcosSDK.build("conf/config.toml");
        Client client = sdk.getClient(1);
        AssembleTransactionProcessor txProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                client,client.getCryptoSuite().createKeyPair());
        //Deploy
        TransactionResponse tr = txProcessor.deployAndGetResponse(ABI, BINARY, Arrays.asList());
        String address  = tr.getContractAddress();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        CountDownLatch latch = new CountDownLatch(txCount);
        String data = txProcessor.encodeFunction(App.ABI, "set", Arrays.asList("name"));
        //Push transaction
        CryptoKeyPair keyPair = new ECDSAKeyPair().generateKeyPair();
        for(int i=0;i<txCount;i++){
            executor.submit(() -> {
                try{
                    String signedTransaction = txProcessor
                            .createSignedTransaction(address, data, keyPair);
                    Object res= jsonClient
                            .invoke("sendRawTransaction", new Object[] { 1,signedTransaction}, Object.class);
                }
                catch (Throwable ex){}
                finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    private static String deploy(AssembleTransactionProcessor txProcessor, String abi, String bin) throws Exception{
        TransactionResponse tr = txProcessor.deployAndGetResponse(abi, bin, Arrays.asList());
        return tr.getContractAddress();
    }


}
