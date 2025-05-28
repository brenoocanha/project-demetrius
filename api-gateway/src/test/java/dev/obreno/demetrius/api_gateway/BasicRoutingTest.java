package dev.obreno.demetrius.api_gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
        , properties = {
            "spring.cloud.gateway.mvc.routes[0].id=mock-service-route",
            "spring.cloud.gateway.mvc.routes[0].uri=http://localhost:9999",
            "spring.cloud.gateway.mvc.routes[0].predicates[0]=Path=/mockservice/**",
            "spring.cloud.gateway.mvc.routes[0].filters[0]=StripPrefix=1"
        })
@ActiveProfiles("test")
@AutoConfigureWebTestClient
public class BasicRoutingTest {
    /* Configures WireMock to be executed in a specific route */
    @RegisterExtension
    static WireMockExtension MOCK_SERVICE = WireMockExtension.newInstance()
            .options(wireMockConfig().port(9999))
            .build();

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenPathMatches_shouldRouteToMockService() {
        /* Configures the expected behavior of the mocked service */
        MOCK_SERVICE.stubFor(WireMock.get(WireMock.urlEqualTo("/data"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Hello from Mock Service!\"}")));

        /* Makes a request through the gateway */
        webTestClient.get().uri("/mockservice/data")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.message").isEqualTo("Hello from Mock Service!");

        /* Checks if the mock service was called as expected */
        MOCK_SERVICE.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/data")));
    }

    @Test
    void whenPathDoesNotMatchAnyRoute_shouldReturn404() {
        webTestClient.get().uri("nonexistentpath")
                .exchange()
                .expectStatus().isNotFound();
    }
}
