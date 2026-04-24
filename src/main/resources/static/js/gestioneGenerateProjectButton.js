(function () {
	const mavenButton = document.getElementById("mavenBtn");
	const gradleButton = document.getElementById("gradleBtn");
	const junitConfigurationModal = document.getElementById("junitConfigurationModal");
	const generateProjectMavenBtn = document.getElementById("generateProjectMavenBtn");
	const generateProjectGradleBtn = document.getElementById("generateProjectGradleBtn");
	let selectedPackageManager = "maven";

	if (!generateProjectMavenBtn || !generateProjectGradleBtn) {
		return;
	}

	const updateGenerateButtons = () => {
		const showMaven = selectedPackageManager === "maven";
		generateProjectMavenBtn.classList.toggle("d-none", !showMaven);
		generateProjectGradleBtn.classList.toggle("d-none", showMaven);
	};

	mavenButton?.addEventListener("click", () => {
		selectedPackageManager = "maven";
		updateGenerateButtons();
	});

	gradleButton?.addEventListener("click", () => {
		selectedPackageManager = "gradle";
		updateGenerateButtons();
	});

	junitConfigurationModal?.addEventListener("show.bs.modal", updateGenerateButtons);
	updateGenerateButtons();
})();
