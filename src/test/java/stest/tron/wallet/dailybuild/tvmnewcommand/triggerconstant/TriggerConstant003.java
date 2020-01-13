package stest.tron.wallet.dailybuild.tvmnewcommand.triggerconstant;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class TriggerConstant003 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;

  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private ManagedChannel channelRealSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubRealSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String realSoliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelRealSolidity = ManagedChannelBuilder.forTarget(realSoliditynode)
        .usePlaintext(true)
        .build();
    blockingStubRealSolidity = WalletSolidityGrpc.newBlockingStub(channelRealSolidity);
  }

  @Test(enabled = true, description = "TriggerConstantContract a view function without ABI")
  public void test001TriggerConstantContract() {
    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/TriggerConstant003.sol";
    String contractName = "testConstantContract";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethed.deployContract(contractName, "[]", code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testView()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

    TransactionExtention transactionExtention1 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testView()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction1 = transactionExtention1.getTransaction();

    byte[] result1 = transactionExtention1.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction1.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention1.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result1));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result1))));

    TransactionExtention transactionExtention2 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testView()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction2 = transactionExtention2.getTransaction();

    byte[] result2 = transactionExtention2.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction2.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention2.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result2));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result2))));
  }


  @Test(enabled = true, description = "TriggerConstantContract a payable function with ABI")
  public void test002TriggerConstantContract() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethed.queryAccount(contractExcKey, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction = transactionExtention.getTransaction();

    byte[] result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));
    Assert.assertEquals("SUCESS", transaction.getRet(0).getRet().toString());
    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            1L, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction = transactionExtention.getTransaction();

    result = transactionExtention.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result))));
    Assert.assertEquals("constant cannot set call value or call token value.",
        ByteArray
            .toStr(transactionExtention.getResult().getMessage().toByteArray()));
    Assert.assertEquals("FAILED", transaction.getRet(0).getRet().toString());


    TransactionExtention transactionExtention1 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction1 = transactionExtention1.getTransaction();

    byte[] result1 = transactionExtention1.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction1.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention1.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result1));
    Assert.assertEquals("SUCESS", transaction1.getRet(0).getRet().toString());
    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result1))));

    transactionExtention1 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            1L, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction1 = transactionExtention1.getTransaction();

    result1 = transactionExtention1.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction1.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention1.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result1));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result1))));
    Assert.assertEquals("constant cannot set call value or call token value.",
        ByteArray
            .toStr(transactionExtention1.getResult().getMessage().toByteArray()));
    Assert.assertEquals("FAILED", transaction1.getRet(0).getRet().toString());


    TransactionExtention transactionExtention2 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            0, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    Transaction transaction2 = transactionExtention2.getTransaction();

    byte[] result2 = transactionExtention2.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction2.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention2.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result2));
    Assert.assertEquals("SUCESS", transaction2.getRet(0).getRet().toString());
    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result2))));

    transactionExtention2 = PublicMethed
        .triggerConstantContractForExtention(contractAddress,
            "testPayable()", "#", false,
            1L, 1000000000, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    transaction2 = transactionExtention2.getTransaction();

    result2 = transactionExtention2.getConstantResult(0).toByteArray();
    System.out.println("message:" + transaction2.getRet(0).getRet());
    System.out.println(":" + ByteArray
        .toStr(transactionExtention2.getResult().getMessage().toByteArray()));
    System.out.println("Result:" + Hex.toHexString(result2));

    Assert.assertEquals(1, ByteArray.toLong(ByteArray
        .fromHexString(Hex
            .toHexString(result2))));
    Assert.assertEquals("constant cannot set call value or call token value.",
        ByteArray
            .toStr(transactionExtention2.getResult().getMessage().toByteArray()));
    Assert.assertEquals("FAILED", transaction2.getRet(0).getRet().toString());
  }
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed
        .freedResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}