package com.codethatmakessense.shop.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void placesPaysAndShipsOverHttp() throws Exception {
        String sku = "API-" + UUID.randomUUID();
        mvc.post().uri("/stock/" + sku + "/receipts").contentType(MediaType.APPLICATION_JSON).content("{\"quantity\": 10}")
                .assertThat().hasStatusOk();

        var placed = mvc.post().uri("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerEmail": "viktor@example.com", "lines": [{"sku": "%s", "quantity": 2, "unitPriceCents": 1500}],
                 "giftWrap": false, "giftMessage": null}
                """.formatted(sku)).exchange();
        assertThat(placed).hasStatus(201);
        long id = Long.parseLong(placed.getResponse().getContentAsString().replaceAll("\\D", ""));

        mvc.post().uri("/orders/" + id + "/payment").contentType(MediaType.APPLICATION_JSON).content("{\"reference\": \"PAY-42\"}")
                .assertThat().hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("PAID");
        mvc.post().uri("/orders/" + id + "/shipments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"carrier\": \"DHL\", \"trackingNumber\": \"TRACK-1\", \"lines\": []}")
                .assertThat().hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("SHIPPED");
        mvc.post().uri("/orders/" + id + "/cancellation").contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"too late\"}")
                .assertThat().hasStatus(409);
        mvc.get().uri("/orders/" + id).assertThat().hasStatusOk().bodyJson().extractingPath("$.totalCents").isEqualTo(3000);
        mvc.get().uri("/stock/" + sku).assertThat().hasStatusOk().bodyJson().extractingPath("$.available").isEqualTo(8);
    }
}