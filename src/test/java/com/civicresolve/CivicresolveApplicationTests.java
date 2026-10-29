package com.civicresolve;

import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
class CivicresolveApplicationTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void rendersServerSideComplaintPagesWithoutJavaScript() throws Exception {
		mockMvc.perform(get("/track"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Track your complaint")));

		mockMvc.perform(get("/report").with(SecurityMockMvcRequestPostProcessors.user(citizenUser())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("name=\"title\"")))
				.andExpect(content().string(containsString("name=\"description\"")))
				.andExpect(content().string(containsString("name=\"location\"")))
				.andExpect(content().string(containsString("AI will identify the right service sector")))
				.andExpect(content().string(containsString("incidentMap")))
				.andExpect(content().string(containsString("map-picker.js")))
				.andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"sector\""))));

		for (String path : List.of("/", "/about", "/features", "/sectors")) {
			mockMvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	private CustomUserDetails citizenUser() {
		return new CustomUserDetails(User.builder()
				.id(1L)
				.firstName("Test")
				.lastName("Citizen")
				.email("citizen@example.test")
				.password("not-used")
				.role(Role.ROLE_CITIZEN)
				.build());
	}
}
