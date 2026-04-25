(function () {
	const mavenButton = document.getElementById("mavenBtn");
	const gradleButton = document.getElementById("gradleBtn");
	const junitConfigurationModal = document.getElementById("junitConfigurationModal");
	const junitConfigurationForm = document.getElementById("junitConfigurationForm");
	const junitConfigurationBackButton = junitConfigurationModal?.querySelector(".modal-back-button");
	const junitConfigurationCloseButton = junitConfigurationModal?.querySelector(".btn-close");
	const generateProjectMavenBtn = document.getElementById("generateProjectMavenBtn");
	const generateProjectGradleBtn = document.getElementById("generateProjectGradleBtn");
	const generatedProjectPanel = document.getElementById("generatedProjectPanel");
	const generatedProjectText = document.getElementById("generatedProjectText");
	let selectedPackageManager = "maven";

	if (!junitConfigurationForm || !generateProjectMavenBtn || !generateProjectGradleBtn || !junitConfigurationModal || !generatedProjectPanel || !generatedProjectText) {
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

	const renderGeneratedProjectState = (content) => {
		generatedProjectPanel.innerHTML = content;
	};

	const showProjectInProgress = () => {
		renderGeneratedProjectState(`
			<p class="generated-project-text mb-0">Project creation in progress...</p>
		`);
	};

	const showProjectError = (message) => {
		renderGeneratedProjectState(`
			<p class="generated-project-text mb-0">${message}</p>
		`);
	};

	const showGeneratedProject = (projectName) => {
		const encodedProjectName = encodeURIComponent(projectName);
		renderGeneratedProjectState(`
			<p class="generated-project-text mb-0">Project created successfully.</p>
			<p class="generated-project-name mb-0">${projectName}</p>
			<div class="generated-project-actions">
				<a class="btn generated-project-button generated-project-button-download" href="/newProject/download?projectName=${encodedProjectName}">Download</a>
				<button type="button" class="btn generated-project-button generated-project-button-delete" data-project-name="${projectName}">Delete</button>
			</div>
		`);
	};

	mavenButton?.addEventListener("click", () => {
		selectedPackageManager = "maven";
		updateGenerateButtons();
	});

	gradleButton?.addEventListener("click", () => {
		selectedPackageManager = "gradle";
		updateGenerateButtons();
	});

	const submitConfigurationForm = async (event) => {
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

		const modalInstance = globalThis.bootstrap?.Modal.getInstance(junitConfigurationModal)
			?? globalThis.bootstrap?.Modal.getOrCreateInstance(junitConfigurationModal);
		modalInstance?.hide();
		showProjectInProgress();

		resetConfigurationForm();

		try {
			const response = await fetch(`${requestUrl}?${searchParams.toString()}`, {
				method: "GET",
				headers: {
					Accept: "application/json"
				}
			});

			if (response.status === 401) {
				globalThis.location.href = "/api/copilot/auth/login";
				return;
			}

			if (!response.ok) {
				throw new Error(`Unable to generate project (${response.status})`);
			}

			const result = await response.json();
			showGeneratedProject(result.projectName ?? searchParams.get("projectName") ?? "Unknown project");
		} catch (error) {
			showProjectError(error instanceof Error ? error.message : "Unable to generate project.");
		}
	};

	generateProjectMavenBtn.addEventListener("click", submitConfigurationForm);
	generateProjectGradleBtn.addEventListener("click", submitConfigurationForm);
	junitConfigurationBackButton?.addEventListener("click", resetConfigurationForm);
	junitConfigurationCloseButton?.addEventListener("click", resetConfigurationForm);
	generatedProjectPanel.addEventListener("click", async (event) => {
		const target = event.target;
		if (!(target instanceof HTMLElement)) {
			return;
		}

		const deleteButton = target.closest(".generated-project-button-delete");
		if (!(deleteButton instanceof HTMLButtonElement)) {
			return;
		}

		const projectName = deleteButton.dataset.projectName;
		if (!projectName) {
			return;
		}

		try {
			const response = await fetch(`/newProject/delete?projectName=${encodeURIComponent(projectName)}`, {
				method: "DELETE",
				headers: {
					Accept: "application/json"
				}
			});

			if (response.status === 401) {
				globalThis.location.href = "/api/copilot/auth/login";
				return;
			}

			if (!response.ok) {
				throw new Error(`Unable to delete project (${response.status})`);
			}

			showProjectError("No project generated yet.");
		} catch (error) {
			showProjectError(error instanceof Error ? error.message : "Unable to delete project.");
		}
	});

	junitConfigurationModal?.addEventListener("show.bs.modal", updateGenerateButtons);
	updateGenerateButtons();
})();
