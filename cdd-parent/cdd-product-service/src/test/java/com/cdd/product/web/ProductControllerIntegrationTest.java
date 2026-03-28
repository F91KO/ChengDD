package com.cdd.product.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cdd.api.product.model.CreateProductRequest;
import com.cdd.api.product.model.CreateSkuRequest;
import com.cdd.api.product.model.InitializeCategoryTreeRequest;
import com.cdd.product.ProductServiceApplication;
import com.cdd.product.service.ProductCatalogApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ProductServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    private static final long DEFAULT_TEMPLATE_ID = 2_000_001L;
    private static final long PREMIUM_FRESH_TEMPLATE_ID = 2_000_002L;
    private static final long COMMUNITY_DELIVERY_TEMPLATE_ID = 2_000_003L;
    private static final long MERCHANT_ID = 3001L;
    private static final long STORE_ID = 4001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductCatalogApplicationService service;

    @Test
    void shouldExposeRichSummaryFieldsForFrontendList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/product/spu")
                        .param("merchant_id", "1001")
                        .param("store_id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = readData(result);
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("page_size").asInt()).isEqualTo(20);
        JsonNode firstProduct = data.path("list").get(0);
        assertThat(firstProduct.path("product_name").asText()).isEqualTo("赣南脐橙礼盒");
        assertThat(firstProduct.path("product_sub_title").asText()).isEqualTo("当季现发 12 枚装");
        assertThat(firstProduct.path("price_summary").path("min_sale_price").decimalValue()).isEqualByComparingTo("59.90");
        assertThat(firstProduct.path("price_summary").path("max_sale_price").decimalValue()).isEqualByComparingTo("59.90");
        assertThat(firstProduct.path("sales_summary").path("total_sales_quantity").asInt()).isEqualTo(1);
        assertThat(firstProduct.path("sales_summary").path("total_sales_amount").decimalValue()).isEqualByComparingTo("59.90");
        assertThat(firstProduct.path("stock_summary").path("total_available_stock").asInt()).isEqualTo(128);
        assertThat(firstProduct.path("stock_summary").path("stock_status").asText()).isEqualTo("in_stock");
        assertThat(firstProduct.path("sku_summaries")).hasSize(1);
        assertThat(firstProduct.path("sku_summaries").get(0).path("sku_code").asText()).isEqualTo("CDD-ORANGE-001");
    }

    @Test
    void shouldSupportProductEditEndpointAndRefreshListSummary() throws Exception {
        service.initializeCategoryTree(new InitializeCategoryTreeRequest(MERCHANT_ID, STORE_ID, DEFAULT_TEMPLATE_ID));
        List<Long> leafCategoryIds = service.listCategories(MERCHANT_ID, STORE_ID).stream()
                .filter(category -> category.categoryLevel() == 2)
                .map(category -> category.id())
                .sorted(Comparator.naturalOrder())
                .toList();
        long initialCategoryId = leafCategoryIds.get(0);
        long updatedCategoryId = leafCategoryIds.get(1);

        var created = service.createProduct(new CreateProductRequest(
                MERCHANT_ID,
                STORE_ID,
                initialCategoryId,
                "待编辑商品",
                "编辑前副标题",
                List.of(new CreateSkuRequest(
                        "EDIT-SKU-001",
                        "默认规格",
                        new BigDecimal("18.80"),
                        30))));
        long originalSkuId = created.skus().get(0).id();

        MvcResult firstUpdateResult = mockMvc.perform(put("/api/product/spu/{product_id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "category_id", updatedCategoryId,
                                "product_name", "编辑后商品",
                                "product_sub_title", "编辑后副标题",
                                "skus", List.of(
                                        Map.of(
                                                "sku_code", "EDIT-SKU-001",
                                                "sku_name", "小份",
                                                "sale_price", new BigDecimal("15.80"),
                                                "available_stock", 25),
                                        Map.of(
                                                "sku_code", "EDIT-SKU-002",
                                                "sku_name", "大份",
                                                "sale_price", new BigDecimal("25.60"),
                                                "available_stock", 45))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.product_name").value("编辑后商品"))
                .andExpect(jsonPath("$.data.product_sub_title").value("编辑后副标题"))
                .andExpect(jsonPath("$.data.category_id").value(updatedCategoryId))
                .andExpect(jsonPath("$.data.skus.length()").value(2))
                .andExpect(jsonPath("$.data.skus[0].id").value(originalSkuId))
                .andReturn();

        JsonNode firstUpdateData = readData(firstUpdateResult);
        JsonNode firstSku = findSku(firstUpdateData.path("skus"), "EDIT-SKU-001");
        JsonNode secondSku = findSku(firstUpdateData.path("skus"), "EDIT-SKU-002");
        assertThat(firstSku.path("id").asLong()).isEqualTo(originalSkuId);
        long secondSkuId = secondSku.path("id").asLong();

        mockMvc.perform(post("/api/product/stock/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "product_id", created.id(),
                                "sku_id", originalSkuId,
                                "delta_stock", -3,
                                "reason", "编辑后库存扣减"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sku_id").value(originalSkuId))
                .andExpect(jsonPath("$.data.available_stock").value(22));

        MvcResult listResult = mockMvc.perform(get("/api/product/spu")
                        .param("merchant_id", String.valueOf(MERCHANT_ID))
                        .param("store_id", String.valueOf(STORE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode listData = readData(listResult);
        JsonNode productNode = findProduct(listData.path("list"), created.id());
        assertThat(productNode.path("product_name").asText()).isEqualTo("编辑后商品");
        assertThat(productNode.path("product_sub_title").asText()).isEqualTo("编辑后副标题");
        assertThat(productNode.path("category_id").asLong()).isEqualTo(updatedCategoryId);
        assertThat(productNode.path("price_summary").path("min_sale_price").decimalValue()).isEqualByComparingTo("15.80");
        assertThat(productNode.path("price_summary").path("max_sale_price").decimalValue()).isEqualByComparingTo("25.60");
        assertThat(productNode.path("sales_summary").path("total_sales_quantity").asInt()).isEqualTo(0);
        assertThat(productNode.path("sales_summary").path("total_sales_amount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(productNode.path("stock_summary").path("total_available_stock").asInt()).isEqualTo(67);
        assertThat(productNode.path("sku_summaries")).hasSize(2);
        assertThat(findSku(productNode.path("sku_summaries"), "EDIT-SKU-001").path("id").asLong()).isEqualTo(originalSkuId);
        assertThat(findSku(productNode.path("sku_summaries"), "EDIT-SKU-001").path("sku_name").asText()).isEqualTo("小份");
        assertThat(findSku(productNode.path("sku_summaries"), "EDIT-SKU-001").path("available_stock").asInt()).isEqualTo(22);
        assertThat(findSku(productNode.path("sku_summaries"), "EDIT-SKU-002").path("id").asLong()).isEqualTo(secondSkuId);
        assertThat(findSku(productNode.path("sku_summaries"), "EDIT-SKU-002").path("sku_name").asText()).isEqualTo("大份");

        MvcResult secondUpdateResult = mockMvc.perform(put("/api/product/spu/{product_id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "merchant_id", MERCHANT_ID,
                                "store_id", STORE_ID,
                                "category_id", updatedCategoryId,
                                "product_name", "二次编辑商品",
                                "product_sub_title", "二次编辑副标题",
                                "skus", List.of(
                                        Map.of(
                                                "sku_code", "EDIT-SKU-001",
                                                "sku_name", "标准份",
                                                "sale_price", new BigDecimal("16.80"),
                                                "available_stock", 20),
                                        Map.of(
                                                "sku_code", "EDIT-SKU-002",
                                                "sku_name", "加大份",
                                                "sale_price", new BigDecimal("26.60"),
                                                "available_stock", 35))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.product_name").value("二次编辑商品"))
                .andExpect(jsonPath("$.data.product_sub_title").value("二次编辑副标题"))
                .andExpect(jsonPath("$.data.skus.length()").value(2))
                .andReturn();

        JsonNode secondUpdateData = readData(secondUpdateResult);
        assertThat(findSku(secondUpdateData.path("skus"), "EDIT-SKU-001").path("id").asLong()).isEqualTo(originalSkuId);
        assertThat(findSku(secondUpdateData.path("skus"), "EDIT-SKU-002").path("id").asLong()).isEqualTo(secondSkuId);
    }

    @Test
    void shouldExposeCategoryTemplatesEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/product/category-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = readData(result);
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("page_size").asInt()).isEqualTo(20);
        JsonNode templates = data.path("list");
        assertThat(templates.isArray()).isTrue();
        assertThat(data.path("total").asInt()).isGreaterThanOrEqualTo(3);
        assertThat(templates.size()).isGreaterThanOrEqualTo(3);

        JsonNode premiumTemplate = findTemplate(templates, PREMIUM_FRESH_TEMPLATE_ID);
        assertThat(premiumTemplate.path("template_name").asText()).isEqualTo("品质精选生鲜模板");
        assertThat(premiumTemplate.path("industry_code").asText()).isEqualTo("fresh_retail");
        assertThat(premiumTemplate.path("categories").isArray()).isTrue();
        assertThat(premiumTemplate.path("categories").size()).isGreaterThan(0);

        JsonNode communityTemplate = findTemplate(templates, COMMUNITY_DELIVERY_TEMPLATE_ID);
        assertThat(communityTemplate.path("template_name").asText()).isEqualTo("社区民生到家模板");
        assertThat(communityTemplate.path("industry_code").asText()).isEqualTo("community_fresh");
        assertThat(communityTemplate.path("categories").isArray()).isTrue();
        assertThat(communityTemplate.path("categories").size()).isGreaterThan(0);
    }

    @Test
    void shouldExposeCategoryListAsPagedTreeOrder() throws Exception {
        service.initializeCategoryTree(new InitializeCategoryTreeRequest(MERCHANT_ID, STORE_ID, DEFAULT_TEMPLATE_ID));

        MvcResult result = mockMvc.perform(get("/api/product/categories")
                        .param("merchant_id", String.valueOf(MERCHANT_ID))
                        .param("store_id", String.valueOf(STORE_ID))
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = readData(result);
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("page_size").asInt()).isEqualTo(20);
        assertThat(data.path("total").asInt()).isGreaterThan(0);
        JsonNode categories = data.path("list");
        assertThat(categories.isArray()).isTrue();
        assertThat(categories.size()).isGreaterThan(0);
        assertThat(categories.get(0).path("parent_id").asLong()).isEqualTo(0L);
        assertThat(categories.get(0).path("category_level").asInt()).isEqualTo(1);
    }

    @Test
    void shouldSupportCategoryKeywordSearch() throws Exception {
        service.initializeCategoryTree(new InitializeCategoryTreeRequest(MERCHANT_ID, STORE_ID, PREMIUM_FRESH_TEMPLATE_ID));

        MvcResult result = mockMvc.perform(get("/api/product/categories")
                        .param("merchant_id", String.valueOf(MERCHANT_ID))
                        .param("store_id", String.valueOf(STORE_ID))
                        .param("keyword", "水果")
                        .param("page", "1")
                        .param("page_size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = readData(result);
        assertThat(data.path("total").asInt()).isGreaterThanOrEqualTo(2);
        JsonNode categories = data.path("list");
        assertThat(categories.isArray()).isTrue();
        assertThat(categories.size()).isGreaterThanOrEqualTo(2);
        for (JsonNode category : categories) {
            assertThat(category.path("category_name").asText()).contains("水果");
        }
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private JsonNode findProduct(JsonNode products, long productId) {
        for (JsonNode product : products) {
            if (product.path("id").asLong() == productId) {
                return product;
            }
        }
        throw new IllegalStateException("未找到商品: " + productId);
    }

    private JsonNode findSku(JsonNode skus, String skuCode) {
        for (JsonNode sku : skus) {
            if (skuCode.equals(sku.path("sku_code").asText())) {
                return sku;
            }
        }
        throw new IllegalStateException("未找到SKU: " + skuCode);
    }

    private JsonNode findTemplate(JsonNode templates, long templateId) {
        for (JsonNode template : templates) {
            if (template.path("id").asLong() == templateId) {
                return template;
            }
        }
        throw new IllegalStateException("未找到分类模板: " + templateId);
    }

    private String writeJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
