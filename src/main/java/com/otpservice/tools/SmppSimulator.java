package com.otpservice.tools;

import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;
import org.jsmpp.util.MessageId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class SmppSimulator {

    private static final int PORT = 2775;
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final AtomicLong msgCounter = new AtomicLong(1);

    public static void main(String[] args) throws IOException {
        System.out.println("[SMPP Simulator] Starting on port " + PORT + "...");
        SMPPServerSessionListener listener = new SMPPServerSessionListener(PORT);
        System.out.println("[SMPP Simulator] Ready. Waiting for connections...");

        while (true) {
            SMPPServerSession session = listener.accept();
            System.out.println("[SMPP Simulator] Client connected");
            executor.submit(() -> handleSession(session));
        }
    }

    private static void handleSession(SMPPServerSession session) {
        try {
            BindRequest bindRequest = session.waitForBind(5000);
            bindRequest.accept("smsc", InterfaceVersion.IF_34);
            System.out.println("[SMPP Simulator] Client bound: " + bindRequest.getSystemId());

            session.setMessageReceiverListener(new ServerMessageReceiverListener() {
                @Override
                public SubmitSmResult onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source)
                        throws ProcessRequestException {
                    String message = new String(submitSm.getShortMessage(), StandardCharsets.UTF_8);
                    System.out.println("[SMPP Simulator] SMS received:");
                    System.out.println("  From : " + submitSm.getSourceAddr());
                    System.out.println("  To   : " + submitSm.getDestAddress());
                    System.out.println("  Text : " + message);
                    try {
                        MessageId id = new MessageId("MSG-" + msgCounter.getAndIncrement());
                        return new SubmitSmResult(id, new OptionalParameter[0]);
                    } catch (Exception ex) {
                        throw new ProcessRequestException(ex.getMessage(), 0);
                    }
                }

                @Override
                public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession source)
                        throws ProcessRequestException {
                    return new SubmitMultiResult("MSG-" + msgCounter.getAndIncrement(), new UnsuccessDelivery[0], new OptionalParameter[0]);
                }

                @Override
                public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source)
                        throws ProcessRequestException {
                    return null;
                }

                @Override
                public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source)
                        throws ProcessRequestException {}

                @Override
                public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source)
                        throws ProcessRequestException {}

                @Override
                public BroadcastSmResult onAcceptBroadcastSm(BroadcastSm broadcastSm, SMPPServerSession source)
                        throws ProcessRequestException {
                    return null;
                }

                @Override
                public void onAcceptCancelBroadcastSm(CancelBroadcastSm cancelBroadcastSm, SMPPServerSession source)
                        throws ProcessRequestException {}

                @Override
                public QueryBroadcastSmResult onAcceptQueryBroadcastSm(QueryBroadcastSm queryBroadcastSm, SMPPServerSession source)
                        throws ProcessRequestException {
                    return null;
                }

                @Override
                public DataSmResult onAcceptDataSm(DataSm dataSm, Session source)
                        throws ProcessRequestException {
                    return null;
                }
            });

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            System.out.println("[SMPP Simulator] Session ended: " + e.getMessage());
        }
    }
}
