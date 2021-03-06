package com.codingapi.txlcn.tc.control.step;

import com.codingapi.txlcn.protocol.await.Lock;
import com.codingapi.txlcn.protocol.await.LockContext;
import com.codingapi.txlcn.protocol.message.event.TransactionCommitEvent;
import com.codingapi.txlcn.protocol.message.event.TransactionJoinEvent;
import com.codingapi.txlcn.tc.config.TxConfig;
import com.codingapi.txlcn.tc.control.TransactionCommitorStrategy;
import com.codingapi.txlcn.tc.control.TransactionState;
import com.codingapi.txlcn.tc.control.TransactionStep;
import com.codingapi.txlcn.tc.exception.TxException;
import com.codingapi.txlcn.tc.info.TransactionInfo;
import com.codingapi.txlcn.tc.reporter.TxManagerReporter;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lorne
 * @date 2020/3/5
 * @description 加入步骤的业务逻辑
 *
 */
@Slf4j
@AllArgsConstructor
public class TransactionStepJoin implements TransactionStep {

    private TxManagerReporter managerProtocoler;

    private TransactionCommitorStrategy transactionCommitorStrategy;

    private TxConfig txConfig;

    @Override
    public TransactionState type() {
        return TransactionState.JOIN;
    }

    @SneakyThrows
    @Override
    public void run(TransactionInfo transactionInfo) {
        // 发送加入事务消息
        long t1 = System.currentTimeMillis();
        TransactionJoinEvent res = (TransactionJoinEvent) managerProtocoler.requestMsg(new TransactionJoinEvent(transactionInfo.getGroupId()));

        if(res==null){
            transactionCommitorStrategy.commit(false);
            throw new TxException("notify transaction fail.");
        }
        long t2 = System.currentTimeMillis();
        log.info("join transaction result:{},time:{}",res.getResult(),(t2-t1));

        //这样要执行groupId等待,等待TM通知事务提交。
        Lock lock = LockContext.getInstance().addKey(transactionInfo.getGroupId());
        if(lock!=null) {
            lock.await(txConfig.getMaxWaitTransactionTime());

            TransactionCommitEvent event = (TransactionCommitEvent) lock.getRes();
            if (event != null) {
                transactionCommitorStrategy.commit(event.isCommit());
            } else {
                //todo 询问TM状态检查
            }
        }

    }
}
