const CANONICAL_LOGOUT_ENDPOINT = '/api/session/logout';
const LOGGED_OUT_PAGE_URL = '/logged-out';

function getLogoutUiElements() {
    return {
        button: document.getElementById('confirmLogoutBtn'),
        spinner: document.getElementById('logoutSpinner'),
        statusBox: document.getElementById('logoutStatusBox'),
        statusIcon: document.getElementById('logoutStatusIcon'),
        message: document.getElementById('logoutMessage')
    };
}

function setLogoutUiState(state, messageText) {
    const { button, spinner, statusBox, statusIcon, message } = getLogoutUiElements();

    if (!statusBox || !statusIcon || !message) {
        return;
    }

    statusBox.className = 'alert border d-flex align-items-start gap-3 mb-4';
    statusIcon.className = 'bi mt-1';

    if (state === 'submitting') {
        statusBox.classList.add('alert-primary');
        statusIcon.classList.add('bi-arrow-repeat', 'text-primary');
        spinner?.classList.remove('d-none');
        if (button) {
            button.disabled = true;
        }
    } else if (state === 'error') {
        statusBox.classList.add('alert-danger');
        statusIcon.classList.add('bi-exclamation-triangle', 'text-danger');
        spinner?.classList.add('d-none');
        if (button) {
            button.disabled = false;
        }
    } else {
        statusBox.classList.add('alert-light');
        statusIcon.classList.add('bi-info-circle', 'text-primary');
        spinner?.classList.add('d-none');
        if (button) {
            button.disabled = false;
        }
    }

    message.textContent = messageText;
}

async function performLogout() {
    setLogoutUiState('submitting', 'Logout in corso. Chiusura della sessione e revoca GitHub, se presente...');

    try {
        const response = await fetch(CANONICAL_LOGOUT_ENDPOINT, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                Accept: 'application/json'
            }
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Logout non completato.');
        }

        const redirectUrl = `${LOGGED_OUT_PAGE_URL}?message=${encodeURIComponent(data.message || 'Hai effettuato il logout correttamente.')}`;
        globalThis.location.assign(redirectUrl);
    } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Errore imprevisto durante il logout.';
        setLogoutUiState('error', errorMessage);
    }
}

globalThis.addEventListener('DOMContentLoaded', () => {
    const { button } = getLogoutUiElements();

    if (button) {
        button.addEventListener('click', performLogout);
    }
});