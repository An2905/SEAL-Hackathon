function Home(props) {

    return (
        <div
            style={{
                textAlign: 'center',
                marginTop: '100px'
            }}
        >

            <h1>SEAL Hackathon</h1>

            <p>
                Welcome to SEAL Hackathon System
            </p>

            <br />

            <button
                onClick={function () {
                    props.setPage('login');
                }}
            >
                Login
            </button>

            <button
                onClick={function () {
                    props.setPage('register');
                }}
                style={{
                    marginLeft: '20px'
                }}
            >
                Register
            </button>

        </div>
    );
}

export default Home;