import { useState } from 'react';

function Register(props) {

    const [fullName, setFullName] = useState('');

    const [email, setEmail] = useState('');

    const [password, setPassword] = useState('');

    const [studentCode, setStudentCode] = useState('');

    const [universityName, setUniversityName] = useState('');

    const [message, setMessage] = useState('');






    async function handleRegister() {

        const response = await fetch(
            'https://localhost:7289/api/auth/register',
            {
                method: 'POST',

                headers: {
                    'Content-Type': 'application/json'
                },

                body: JSON.stringify({
                    fullName: fullName,
                    email: email,
                    password: password,
                    studentCode: studentCode,
                    universityName: universityName,
                    role: 'STUDENT'
                })
            }
        );






        const data = await response.text();

        setMessage(data);
    }






    return (
        <div
            style={{
                textAlign: 'center',
                marginTop: '100px'
            }}
        >

            <h1>Register</h1>






            <input
                type="text"
                placeholder="Full Name"
                onChange={(e) => setFullName(e.target.value)}
            />

            <br /><br />






            <input
                type="text"
                placeholder="Email"
                onChange={(e) => setEmail(e.target.value)}
            />

            <br /><br />






            <input
                type="password"
                placeholder="Password"
                onChange={(e) => setPassword(e.target.value)}
            />

            <br /><br />






            <input
                type="text"
                placeholder="Student Code"
                onChange={(e) => setStudentCode(e.target.value)}
            />

            <br /><br />






            <input
                type="text"
                placeholder="University Name"
                onChange={(e) => setUniversityName(e.target.value)}
            />

            <br /><br />






            <button onClick={handleRegister}>
                Register
            </button>

            <button
                onClick={function () {
                    props.setPage('home');
                }}
                style={{
                    marginLeft: '20px'
                }}
            >
                Back
            </button>






            <p>{message}</p>

        </div>
    );
}

export default Register;