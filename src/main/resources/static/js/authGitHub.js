let pollTimer = null;

const STATUS_STYLE_MAP = {
    loading: {
        container: ['border-primary-subtle', 'bg-primary-subtle'],
        message: ['text-primary-emphasis'],
        spinnerClass: 'text-primary',
        spinnerVisible: true
    },
    pending: {
        container: ['border-primary-subtle', 'bg-primary-subtle'],
        message: ['text-primary-emphasis'],
        spinnerClass: 'text-primary',
        spinnerVisible: true
    },
    authorized: {
        container: ['border-success-subtle', 'bg-success-subtle'],
        message: ['text-success-emphasis', 'fw-semibold'],
        spinnerClass: 'text-success',
        spinnerVisible: false
    },
    expired: {
        container: ['border-warning-subtle', 'bg-warning-subtle'],
        message: ['text-warning-emphasis', 'fw-semibold'],
        spinnerClass: 'text-warning',
        spinnerVisible: false
    },
    denied: {
        container: ['border-danger-subtle', 'bg-danger-subtle'],
        message: ['text-danger-emphasis', 'fw-semibold'],
        spinnerClass: 'text-danger',
        spinnerVisible: false
    },
    error: {
        container: ['border-danger-subtle', 'bg-danger-subtle'],
        message: ['text-danger-emphasis', 'fw-semibold'],
        spinnerClass: 'text-danger',
        spinnerVisible: false
    }
};

function stopPolling() {
    if (pollTimer) {
        clearTimeout(pollTimer);
        pollTimer = null;
    }
}

function schedulePoll(seconds) {
    stopPolling();
    const delaySeconds = Number.isFinite(seconds) && seconds > 0 ? seconds : 5;
    pollTimer = setTimeout(checkStatus, delaySeconds * 1000);
}

function getUiElements() {
    return {
        message: document.getElementById('message'),
        deviceBox: document.getElementById('deviceBox'),
        verifyLink: document.getElementById('verifyLink'),
        userCode: document.getElementById('userCode'),
        statusBox: document.querySelector('.auth-status'),
        spinner: document.querySelector('.auth-status .spinner-border')
    };
}

function resetStatusStyles(statusBox, message, spinner) {
    if (statusBox) {
        statusBox.classList.remove(
            'border-primary-subtle',
            'bg-primary-subtle',
            'border-success-subtle',
            'bg-success-subtle',
            'border-warning-subtle',
            'bg-warning-subtle',
            'border-danger-subtle',
            'bg-danger-subtle'
        );
    }

    if (message) {
        message.classList.remove(
            'text-secondary',
            'text-primary-emphasis',
            'text-success-emphasis',
            'text-warning-emphasis',
            'text-danger-emphasis',
            'fw-semibold'
        );
    }

    if (spinner) {
        spinner.classList.remove(
            'text-primary',
            'text-success',
            'text-warning',
            'text-danger',
            'd-none'
        );
    }
}

function applyStatusStyle(status, messageText) {
    const { message, statusBox, spinner } = getUiElements();
    const style = STATUS_STYLE_MAP[status] || STATUS_STYLE_MAP.error;

    resetStatusStyles(statusBox, message, spinner);

    if (statusBox) {
        statusBox.classList.add(...style.container);
    }

    if (message) {
        message.textContent = messageText || 'Stato non disponibile.';
        message.classList.add(...style.message);
    }

    if (spinner) {
        spinner.classList.add(style.spinnerClass);

        if (!style.spinnerVisible) {
            spinner.classList.add('d-none');
        }
    }
}

function showDeviceBox() {
    const { deviceBox } = getUiElements();
    if (deviceBox) {
        deviceBox.classList.remove('hidden');
    }
}

function hideDeviceBox() {
    const { deviceBox } = getUiElements();
    if (deviceBox) {
        deviceBox.classList.add('hidden');
    }
}

async function startDeviceFlow() {
    applyStatusStyle('loading', 'Preparazione del codice di accesso...');

    const response = await fetch('/api/copilot/auth/device/start', {
        method: 'GET',
        credentials: 'same-origin',
        headers: {
            Accept: 'application/json'
        }
    });

    const data = await response.json();

    if (!response.ok || data.status === 'error') {
        applyStatusStyle(
            'error',
            data.message || "Errore durante l'avvio del Device Flow."
        );
        return;
    }

    const { verifyLink, userCode } = getUiElements();

    if (verifyLink) {
        verifyLink.href = data.verificationUri;
    }

    if (userCode) {
        userCode.textContent = data.userCode;
    }

    showDeviceBox();
    applyStatusStyle('pending', data.message || 'Autorizzazione in attesa su GitHub.');

    schedulePoll(data.pollIntervalSeconds);
}

async function checkStatus() {
    const response = await fetch('/api/copilot/auth/device/status', {
        method: 'GET',
        credentials: 'same-origin',
        headers: {
            Accept: 'application/json'
        }
    });

    const data = await response.json();

    if (data.status === 'pending') {
        showDeviceBox();
        applyStatusStyle(
            'pending',
            data.message || 'Autorizzazione in attesa su GitHub.'
        );
        schedulePoll(data.pollAfterSeconds || 5);
        return;
    }

    stopPolling();

    if (data.status === 'authorized') {
        hideDeviceBox();
        applyStatusStyle(
            'authorized',
            data.message || 'Autenticazione GitHub completata correttamente.'
        );
        const homeBox = document.getElementById('homeBox');
        if (homeBox) {
            homeBox.classList.remove('hidden');
        }
        return;
    }

    if (data.status === 'expired') {
        showDeviceBox();
        applyStatusStyle(
            'expired',
            (data.message || 'Codice scaduto.') + ' Ricarica la pagina per generare un nuovo codice.'
        );
        return;
    }

    if (data.status === 'denied') {
        showDeviceBox();
        applyStatusStyle(
            'denied',
            data.message || "Autorizzazione negata dall'utente."
        );
        return;
    }

    showDeviceBox();
    applyStatusStyle(
        'error',
        data.message || 'Errore durante il controllo dello stato del Device Flow.'
    );
}

window.addEventListener('DOMContentLoaded', () => {
    startDeviceFlow().catch(error => {
        stopPolling();
        showDeviceBox();
        applyStatusStyle(
            'error',
            "Errore durante l'avvio del Device Flow: " + error
        );
    });
});