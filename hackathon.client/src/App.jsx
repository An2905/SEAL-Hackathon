import { useState } from 'react';

import Home from './Home';
import Login from './Login';
import Register from './Register';

function App() {

    const [page, setPage] = useState('home');

    if (page === 'login') {
        return <Login setPage={setPage} />;
    }

    if (page === 'register') {
        return <Register setPage={setPage} />;
    }

    return <Home setPage={setPage} />;
}

export default App;