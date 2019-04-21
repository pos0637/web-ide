import { Mock } from '~/components/request';

Mock.onPost(/api\/v1\/debugger\/start[?.*]?/).reply(() =>
    [200, {
        code: 200,
        data: {}
    }]
);

Mock.onGet(/api\/v1\/debugger\/codes[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: '',
        newTRoken: null
    }]
);

Mock.onGet(/api\/v1\/debugger\/state[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: 'Idle',
        newTRoken: null
    }]
);

Mock.onGet(/api\/v1\/debugger\/location[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: null,
        newTRoken: null
    }]
);

Mock.onGet(/api\/v1\/debugger\/stack[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: null,
        newTRoken: null
    }]
);

Mock.onGet(/api\/v1\/debugger\/variables[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: null,
        newTRoken: null
    }]
);

Mock.onGet(/api\/v1\/debugger\/breakpoints[?.*]?/).reply(() =>
    [200, {
        code: 200,
        errno: 0,
        message: null,
        data: null,
        newTRoken: null
    }]
);
