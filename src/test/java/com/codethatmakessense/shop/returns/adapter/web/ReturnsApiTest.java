package com.codethatmakessense.shop.returns.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.codethatmakessense.shop.BootsSpring;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@BootsSpring
@SpringBootTest
@AutoConfigureMockMvc
class ReturnsApiTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void walksAReturnToRefundOverHttp() throws Exception {
        String sku = "RET-" + UUID.randomUUID();
        mvc.post().uri("/stock/" + sku + "/receipts").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 10}").exchange();
        var placed = mvc.post().uri("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerEmail": "viktor@example.com", "lines": [{"sku": "%s", "quantity": 2, "unitPriceCents": 1500}],
                 "giftWrap": false, "giftMessage": null}
                """.formatted(sku)).exchange();
        long orderId = Long.parseLong(placed.getResponse().getContentAsString().replaceAll("\\D", ""));
        mvc.post().uri("/orders/" + orderId + "/payment").contentType(MediaType.APPLICATION_JSON).content("{\"reference\": \"PAY-42\"}").exchange();
        mvc.post().uri("/orders/" + orderId + "/shipments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"carrier\": \"DHL\", \"trackingNumber\": \"TRACK-1\", \"lines\": []}").exchange();

        var requested = mvc.post().uri("/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": " + orderId + ", \"sku\": \"" + sku + "\", \"quantity\": 1}").exchange();
        assertThat(requested).hasStatus(201).bodyJson().extractingPath("$.refundCents").isEqualTo(1500);
        String id = requested.getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mvc.post().uri("/returns/" + id + "/approval").assertThat().hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("APPROVED");
        mvc.post().uri("/returns/" + id + "/receipt").assertThat().hasStatusOk();
        mvc.post().uri("/returns/" + id + "/refund").assertThat().hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("REFUNDED");
        mvc.post().uri("/returns").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": " + orderId + ", \"sku\": \"RET-MUG\", \"quantity\": 1}").assertThat().hasStatus(409);
    }
}