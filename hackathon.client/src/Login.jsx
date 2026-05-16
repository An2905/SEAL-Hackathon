import { useState } from 'react';

function Login(props) {

    const [email, setEmail] = useState('');

    const [password, setPassword] = useState('');

    const [message, setMessage] = useState('');






    async function handleLogin() {

        const response = await fetch(
            'https://localhost:7289/api/auth/login',
            {
                method: 'POST',

                headers: {
                    'Content-Type': 'application/json'
                },

                body: JSON.stringify({
                    email: email,
                    password: password
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

            <h1>Login</h1>

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

            <button onClick={handleLogin}>
                Login
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

export default Login;