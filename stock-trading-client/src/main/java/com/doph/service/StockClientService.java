package com.doph.service;

import com.doph.grpc.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockClientService {

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub serviceBlockingStub;

    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceStub asyncStub;

    // StockResponse getStockPrice(StockRequest)
    public StockResponse getStockPrice(String stockSymbol){
        StockRequest request = StockRequest.newBuilder().setStockSymbol(stockSymbol).build();
        return serviceBlockingStub.getStockPrice(request);
    }

    public void subscribeStockPrice(String stockSymbol){
        StockRequest request = StockRequest.newBuilder().setStockSymbol(stockSymbol).build();
        asyncStub.subscribeStockPrice(request, new StreamObserver<>(){

            @Override
            public void onNext(StockResponse stockResponse) {
                System.out.println("Stock Price Updated: "+stockResponse.getStockSymbol() +
                        " Price: " + stockResponse.getPrice() +
                        " Time: " + stockResponse.getTimestamp());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed");
            }
        });
    }

    public void placeBulkOrders(){
        StreamObserver<OrderSummary> responseObserver = new StreamObserver<OrderSummary>() {
            @Override
            public void onNext(OrderSummary summary) {
                System.out.println("Order Summary Received from Server:");
                System.out.println("Total Orders: "+summary.getTotalOrders());
                System.out.println("Success Orders: "+summary.getSuccessCount());
                System.out.println("Total Amount: $"+summary.getTotalAmount());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Order Summary Received from Server: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed, server is done sending summary!");
            }
        };

        StreamObserver<StockOrder> requestObserver = asyncStub.bulkStockOrder(responseObserver);

        List<StockOrder> mockOrders = List.of(
                StockOrder.newBuilder()
                        .setOrderId("ORD-001")
                        .setStockSymbol("AAPL")
                        .setQuantity(10)
                        .setPrice(228.50)
                        .setOrderType("BUY")
                        .build(),
                StockOrder.newBuilder()
                        .setOrderId("ORD-002")
                        .setStockSymbol("NVDA")
                        .setQuantity(5)
                        .setPrice(142.65)
                        .setOrderType("BUY")
                        .build(),
                StockOrder.newBuilder()
                        .setOrderId("ORD-003")
                        .setStockSymbol("TSLA")
                        .setQuantity(3)
                        .setPrice(315.40)
                        .setOrderType("SELL")
                        .build(),
                StockOrder.newBuilder()
                        .setOrderId("ORD-004")
                        .setStockSymbol("GOOGL")
                        .setQuantity(8)
                        .setPrice(176.20)
                        .setOrderType("BUY")
                        .build(),
                StockOrder.newBuilder()
                        .setOrderId("ORD-005")
                        .setStockSymbol("MSFT")
                        .setQuantity(2)
                        .setPrice(425.80)
                        .setOrderType("SELL")
                        .build()
        );

        try {
            for (StockOrder order : mockOrders) {
                System.out.println("Sending order: " + order.getOrderId() + " - " + order.getStockSymbol());
                requestObserver.onNext(order);
                Thread.sleep(500);
            }

            // done sending message
            requestObserver.onCompleted();

        }catch (Exception ex){
            requestObserver.onError(ex);
        }
    }

    public void startLiveTrading() throws InterruptedException {
        StreamObserver<StockOrder> requestObserver = asyncStub.liveTrading(new StreamObserver<TradeStatus>() {
            @Override
            public void onNext(TradeStatus status) {
                System.out.println("Server response: "+status);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error: "+t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Stream completed.");
            }
        });

        List<StockOrder> liveOrders = List.of(
                StockOrder.newBuilder().setOrderId("ORD-101").setStockSymbol("AAPL").setQuantity(10).setPrice(228.50).setOrderType("BUY").build(),
                StockOrder.newBuilder().setOrderId("ORD-102").setStockSymbol("NVDA").setQuantity(5).setPrice(142.65).setOrderType("BUY").build(),
                StockOrder.newBuilder().setOrderId("ORD-103").setStockSymbol("TSLA").setQuantity(2).setPrice(315.40).setOrderType("SELL").build(),
                StockOrder.newBuilder().setOrderId("ORD-104").setStockSymbol("GOOGL").setQuantity(0).setPrice(176.20).setOrderType("BUY").build(),
                StockOrder.newBuilder().setOrderId("ORD-105").setStockSymbol("MSFT").setQuantity(4).setPrice(425.80).setOrderType("SELL").build(),
                StockOrder.newBuilder().setOrderId("ORD-106").setStockSymbol("FPT").setQuantity(100).setPrice(136500).setOrderType("BUY").build()
        );

        // sending multiple order request from client
        try {
            for (StockOrder order : liveOrders) {
                System.out.printf("[CLIENT ->] Sending %-8s | %-4s %3d %-5s @ %.2f%n",
                        order.getOrderId(),
                        order.getOrderType(),
                        order.getQuantity(),
                        order.getStockSymbol(),
                        order.getPrice());

                requestObserver.onNext(order);
                Thread.sleep(800);
            }
        } catch (Exception e) {
            requestObserver.onError(e);
            return;
        }
        requestObserver.onCompleted();
    }
}
