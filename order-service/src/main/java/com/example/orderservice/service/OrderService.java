package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
/*
yuxariki metodda bir error almisdim, return tipini String tipinde elemisdim
ve end pointimin tipi Mono<Strin> oldugundan error verirdi
*/
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    /*public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrdernumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtos()
                .stream()
                .map(this::mapToOrderLineItems).toList();
        order.setOrderLineItems(orderLineItems);
// bu setirde orderlineItemsin icinden skucodelari cixardiv skuCodes deyiskenine map edirik
        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkucode).toList();

        ///   !!!!!!!!! Inter   process Communucation
        InventoryResponse[] inventoryResponses = webClient.get()                // cunki inventory controllerden alacagimiz end point get tipindedi
                .uri("http://localhost:8082/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())  //enpoint RequestParam teleb elediyinden teleblerini doldururuq
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();        //webclient normalda asynchron request qebul edir, block vasitesiyle synchron request qebul edir

        boolean allProductsIsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

        if (allProductsIsInStock) {
            orderRepository.save(order);
        }
        else {
            throw new IllegalArgumentException("Product is not in stock");
        }
    }*/
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrdernumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtos()
                .stream()
                .map(this::mapToOrderLineItems)
                .toList();
        order.setOrderLineItems(orderLineItems);

        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkucode)
                .toList();

        boolean allProductsInStock = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToFlux(InventoryResponse.class)
                .all(InventoryResponse::isInStock)
                .block(); // Blocking operation to wait for completion

        if (allProductsInStock) {
            orderRepository.save(order);
            return "Order is placed";
        } else {
            throw new IllegalArgumentException("Product is not in stock");
        }
    }





    private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setSkucode(orderLineItemsDto.getSkucode());
        return orderLineItems;
    }
}
