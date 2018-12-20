const electron = require('electron');

const { app, BrowserWindow } = electron;

const isDevelopment = false;
if (isDevelopment) {
    require('electron-reload')(__dirname, {
        ignored: /node_modules|[\/\\]\./
    });
}


let mainWnd = null;

function createMainWnd() {
    mainWnd = new BrowserWindow({
        width: 4096,
        height: 2160,
        fullscreen: true,
        icon: 'public/img/app-icon.png'
    });

    if (isDevelopment) {
        mainWnd.webContents.openDevTools();
    }

    mainWnd.loadURL(`file://${__dirname}/dist/index.html`);

    mainWnd.on('closed', () => {
        mainWnd = null;
    });
}

app.on('ready', createMainWnd);

app.on('window-all-closed', () => {
    app.quit();
});