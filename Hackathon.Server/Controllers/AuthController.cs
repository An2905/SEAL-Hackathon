using Microsoft.AspNetCore.Mvc;//dùng cho API Controller
using Microsoft.Data.SqlClient;//kết nối SQL Server
using Microsoft.IdentityModel.Tokens;//dùng cho JWT security, tạo secret key, signing credentials
using SEALHackathon.Server.Models;//
using System.IdentityModel.Tokens.Jwt;//tạo JWT token
using System.Security.Claims;//tạo dữ liệu bên trong JWT
using System.Text;//dùng Encoding.UTF8.GetBytes(...) để convert secret key thành bytes cho JWT.

namespace SEALHackathon.Server.Controllers
{
    [ApiController]
    [Route("api/auth")]
    public class AuthController : ControllerBase
    {
        private string connectionString ="Server=localhost;Database=Hackathon;Trusted_Connection=True;TrustServerCertificate=True";
        private IConfiguration configuration;
        public AuthController(IConfiguration _configuration)
        {
            configuration = _configuration;
        }








        [HttpPost("login")]
        public object Login([FromBody] LoginRequest request)
        {
            SqlConnection connection = new SqlConnection(connectionString);
            connection.Open();






            string sql = "SELECT * FROM users WHERE email = @Email AND password_hash = @Password";

            SqlCommand command = new SqlCommand(sql, connection);

            command.Parameters.AddWithValue("@Email", request.Email);

            command.Parameters.AddWithValue("@Password", request.Password);

            SqlDataReader reader = command.ExecuteReader();






            if (reader.Read())
            {
                string email = reader["email"].ToString();

                string role = reader["role"].ToString();


                Claim[] claims =
                {
                    new Claim("email", email),
                    new Claim("role", role)
                };


                var key =
                    new SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(
                            configuration["Jwt:Key"]
                        )
                    );

                var credentials =
                    new SigningCredentials(key,SecurityAlgorithms.HmacSha256);

                var token =new JwtSecurityToken(claims: claims,expires:DateTime.Now.AddHours(2), signingCredentials:credentials);






                string jwtToken =new JwtSecurityTokenHandler().WriteToken(token);


                reader.Close();
                connection.Close();


                return new
                {
                    message = "Login success",
                    token = jwtToken,
                    role = role
                };
            }






            reader.Close();
            connection.Close();






            return new
            {
                message = "Wrong email or password"
            };
        }









        [HttpPost("register")]
        public string Register([FromBody] RegisterRequest request)
        {
            SqlConnection connection =
                new SqlConnection(connectionString);
            connection.Open();






            string checkEmailSql = "SELECT * FROM users WHERE email = @Email";

            SqlCommand checkEmailCommand = new SqlCommand(checkEmailSql, connection);

            checkEmailCommand.Parameters.AddWithValue("@Email", request.Email);

            SqlDataReader reader = checkEmailCommand.ExecuteReader();






            if (reader.Read())
            {
                reader.Close();

                connection.Close();

                return "Email already exists";
            }

            reader.Close();






            string insertUserSql ="INSERT INTO users(full_name, email, password_hash, role, status) OUTPUT INSERTED.user_id VALUES(@FullName, @Email, @Password, 'STUDENT', 'APPROVED')";

            SqlCommand insertUserCommand = new SqlCommand(insertUserSql, connection);

            insertUserCommand.Parameters.AddWithValue("@FullName", request.FullName);

            insertUserCommand.Parameters.AddWithValue("@Email", request.Email);

            insertUserCommand.Parameters.AddWithValue("@Password", request.Password);






            int userID =Convert.ToInt32(insertUserCommand.ExecuteScalar());






            string insertProfileSql ="INSERT INTO studentProfile(user_id, student_code, university_name) VALUES(@UserID, @StudentCode, @UniversityName)";

            SqlCommand insertProfileCommand = new SqlCommand(insertProfileSql, connection);

            insertProfileCommand.Parameters.AddWithValue("@UserID", userID);

            insertProfileCommand.Parameters.AddWithValue("@StudentCode", request.StudentCode);

            insertProfileCommand.Parameters.AddWithValue("@UniversityName", request.UniversityName);

            insertProfileCommand.ExecuteNonQuery();






            connection.Close();

            return "Register success";
        }
    }
}