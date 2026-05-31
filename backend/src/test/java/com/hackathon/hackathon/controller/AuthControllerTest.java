package com.hackathon.hackathon.controller;

import com.hackathon.hackathon.model.dto.request.LoginRequest;
import com.hackathon.hackathon.service.AuthService;
import mockit.Injectable;
import mockit.Tested;
import mockit.Expectations;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthControllerTest {

    @Tested
    private AuthController authController;

    @Injectable
    private AuthService authService;

    @Injectable
    private BCryptPasswordEncoder encoder;

    // ====== TEST STUDENT ROLE ======
    @Test
    public void testLoginStudentSuccess() {
        // Tạo request giả lập login
        LoginRequest request = new LoginRequest();

        // Set email cho request
        request.setEmail("student1@fpt.edu.vn");

        // Set password cho request
        request.setPassword("123456");

        // Kết quả mong đợi sau khi login thành công
        String expectedResponse = "{\"message\":\"Login success\",\"token\":\"student_token_abc123\"}";

        // Expectations:

        // Define behavior cho mock object
        new Expectations() {
            {
                // Nếu authService.login(request) được gọi
                authService.login(request);

                // thì trả về expectedResponse
                result = expectedResponse;
            }
        };

        // Gọi method login của controller
        ResponseEntity<String> response = authController.login(request);

        // Kiểm tra:
        // response thực tế có giống expectedResponse không
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());

        // In thông báo PASS ra terminal
        System.out.println("✓ Test STUDENT login success");
    }

    // ====== TEST MENTOR ROLE ======
    @Test
    public void testLoginMentorSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("mentor@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Login success\",\"token\":\"mentor_token_def456\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test MENTOR login success");
    }

    // ====== TEST JUDGE ROLE ======
    @Test
    public void testLoginJudgeSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("judge@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Login success\",\"token\":\"judge_token_ghi789\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test JUDGE login success");
    }

    // ====== TEST STAFF ROLE ======
    @Test
    public void testLoginStaffSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("staff@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Login success\",\"token\":\"staff_token_jkl101112\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test STAFF login success");
    }

    // ====== TEST ERROR CASES ======
    @Test
    public void testLoginEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("notexist@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Email not found\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test email not found");
    }

    @Test
    public void testLoginWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@fpt.edu.vn");
        request.setPassword("wrongpassword");

        String expectedResponse = "{\"message\":\"Wrong password\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test wrong password");
    }

    @Test
    public void testLoginAccountInactive() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Account is inactive\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test account inactive");
    }

    @Test
    public void testLoginDatabaseError() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@fpt.edu.vn");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Database error\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test database error");
    }

    // ====== TEST MULTIPLE LOGINS (CONCURRENT) ======
    @Test
    public void testMultipleStudentLogins() {
        LoginRequest request1 = new LoginRequest();
        request1.setEmail("student1@fpt.edu.vn");
        request1.setPassword("pass1");

        LoginRequest request2 = new LoginRequest();
        request2.setEmail("student2@fpt.edu.vn");
        request2.setPassword("pass2");

        String response1 = "{\"message\":\"Login success\",\"token\":\"token1\"}";
        String response2 = "{\"message\":\"Login success\",\"token\":\"token2\"}";

        new Expectations() {
            {
                authService.login(request1);
                result = response1;
                authService.login(request2);
                result = response2;
            }
        };

        ResponseEntity<String> actual1 = authController.login(request1);
        ResponseEntity<String> actual2 = authController.login(request2);

        assertEquals(200, actual1.getStatusCode().value());
        assertEquals(response1, actual1.getBody());
        assertEquals(200, actual2.getStatusCode().value());
        assertEquals(response2, actual2.getBody());
        System.out.println("✓ Test multiple student logins");
    }

    // ====== TEST MIXED ROLES ======
    @Test
    public void testMixedRolesLogin() {
        LoginRequest studentReq = new LoginRequest();
        studentReq.setEmail("student@fpt.edu.vn");
        studentReq.setPassword("pass");

        LoginRequest mentorReq = new LoginRequest();
        mentorReq.setEmail("mentor@fpt.edu.vn");
        mentorReq.setPassword("pass");

        LoginRequest judgeReq = new LoginRequest();
        judgeReq.setEmail("judge@fpt.edu.vn");
        judgeReq.setPassword("pass");

        String studentRes = "{\"message\":\"Login success\",\"token\":\"student_t\"}";
        String mentorRes = "{\"message\":\"Login success\",\"token\":\"mentor_t\"}";
        String judgeRes = "{\"message\":\"Login success\",\"token\":\"judge_t\"}";

        new Expectations() {
            {
                authService.login(studentReq);
                result = studentRes;
                authService.login(mentorReq);
                result = mentorRes;
                authService.login(judgeReq);
                result = judgeRes;
            }
        };

        assertEquals(studentRes, authController.login(studentReq).getBody());
        assertEquals(mentorRes, authController.login(mentorReq).getBody());
        assertEquals(judgeRes, authController.login(judgeReq).getBody());
        System.out.println("✓ Test mixed roles login");
    }

    // ====== TEST EDGE CASES ======
    @Test
    public void testLoginWithEmptyEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Email is required\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test empty email");
    }

    @Test
    public void testLoginWithEmptyPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@fpt.edu.vn");
        request.setPassword("");

        String expectedResponse = "{\"message\":\"Password is required\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test empty password");
    }

    @Test
    public void testLoginWithInvalidEmailFormat() {
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        String expectedResponse = "{\"message\":\"Invalid email format\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test invalid email format");
    }

    @Test
    public void testLoginWithNullRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPassword(null);

        String expectedResponse = "{\"message\":\"Request is invalid\"}";

        new Expectations() {
            {
                authService.login(request);
                result = expectedResponse;
            }
        };

        ResponseEntity<String> response = authController.login(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedResponse, response.getBody());
        System.out.println("✓ Test null request");
    }
}
