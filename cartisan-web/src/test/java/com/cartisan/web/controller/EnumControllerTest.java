package com.cartisan.web.controller;

import com.cartisan.web.response.EnumOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnumController.class)
class EnumControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private com.cartisan.web.enums.EnumRegistry enumRegistry;

    @Test
    void shouldGetSingleEnum() throws Exception {
        // given
        when(enumRegistry.getEnumOptions("TestUserStatus"))
            .thenReturn(List.of(new EnumOption(1, "启用")));

        // when & then
        mvc.perform(get("/api/enums/TestUserStatus"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].code").value(1))
            .andExpect(jsonPath("$.data[0].name").value("启用"));
    }

    @Test
    void shouldBatchGetEnums() throws Exception {
        // given
        when(enumRegistry.getEnumOptions("TestUserStatus"))
            .thenReturn(List.of(new EnumOption(1, "启用")));

        String json = "{\"enums\":[\"TestUserStatus\"]}";

        // when & then
        mvc.perform(post("/api/enums/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.enums.TestUserStatus").isArray())
            .andExpect(jsonPath("$.data.enums.TestUserStatus[0].code").value(1));
    }

    @Test
    void shouldReturn404WhenEnumNotFound() throws Exception {
        // given
        when(enumRegistry.getEnumOptions("NotExist"))
            .thenThrow(new IllegalArgumentException("Enum not found: NotExist"));

        // when & then
        mvc.perform(get("/api/enums/NotExist"))
            .andExpect(status().isInternalServerError());
    }
}