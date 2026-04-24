(function () {
	const mavenButton = document.getElementById("mavenBtn");
	const gradleButton = document.getElementById("gradleBtn");
	const junitConfigurationModal = document.getElementById("junitConfigurationModal");
	const junitConfigurationForm = document.getElementById("junitConfigurationForm");
	const junitConfigurationCloseButton = junitConfigurationModal?.querySelector(".btn-close");
	const generateProjectMavenBtn = document.getElementById("generateProjectMavenBtn");
	const generateProjectGradleBtn = document.getElementById("generateProjectGradleBtn");
	let selectedPackageManager = "maven";

	if (!junitConfigurationForm || !generateProjectMavenBtn || !generateProjectGradleBtn) {
		return;
	}

	const updateGenerateButtons = () => {
		const showMaven = selectedPackageManager === "maven";
		generateProjectMavenBtn.classList.toggle("d-none", !showMaven);
		generateProjectGradleBtn.classList.toggle("d-none", showMaven);
	};

	const resetConfigurationForm = () => {
		junitConfigurationForm.reset();
	};

	mavenButton?.addEventListener("click", () => {
		selectedPackageManager = "maven";
		updateGenerateButtons();
	});

	gradleButton?.addEventListener("click", () => {
		selectedPackageManager = "gradle";
		updateGenerateButtons();
	});

	const submitConfigurationForm = (event) => {
		event.preventDefault();

		const submitButton = event.currentTarget;
		if (!(submitButton instanceof HTMLButtonElement)) {
			return;
		}

		const requestUrl = submitButton.getAttribute("formaction");
		if (!requestUrl) {
			return;
		}

		const formData = new FormData(junitConfigurationForm);
		const searchParams = new URLSearchParams();

		for (const [key, value] of formData.entries()) {
			searchParams.append(key, String(value));
		}

		resetConfigurationForm();

		globalThis.location.href = `${requestUrl}?${searchParams.toString()}`;
	};

	generateProjectMavenBtn.addEventListener("click", submitConfigurationForm);
	generateProjectGradleBtn.addEventListener("click", submitConfigurationForm);
	junitConfigurationCloseButton?.addEventListener("click", resetConfigurationForm);

	junitConfigurationModal?.addEventListener("show.bs.modal", updateGenerateButtons);
	updateGenerateButtons();
})();
