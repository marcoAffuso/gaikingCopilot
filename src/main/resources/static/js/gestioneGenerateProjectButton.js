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
	const projectNameInput = document.getElementById("projectName");
	const groupIdInput = document.getElementById("groupId");
	const createGitProjectModal = document.getElementById("createGitProjectModal");
	const createGitProjectName = document.getElementById("createGitProjectName");
	const validationAlertOverlay = document.getElementById("validationAlertOverlay");
	const validationAlertMessage = document.getElementById("validationAlertMessage");
	const validationAlertCloseBtn = document.getElementById("validationAlertCloseBtn");
	const validationAlertConfirmBtn = document.getElementById("validationAlertConfirmBtn");
	let selectedPackageManager = "maven";

	if (!junitConfigurationForm || !generateProjectMavenBtn || !generateProjectGradleBtn || !junitConfigurationModal || !generatedProjectPanel || !generatedProjectText || !projectNameInput || !groupIdInput || !createGitProjectModal || !createGitProjectName || !validationAlertOverlay || !validationAlertMessage || !validationAlertCloseBtn || !validationAlertConfirmBtn) {
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

	const hasEmptyConfigurationTextFields = () => {
		const textInputs = junitConfigurationForm.querySelectorAll('input[type="text"]');

		for (const textInput of textInputs) {
			if (!(textInput instanceof HTMLInputElement)) {
				continue;
			}

			if (textInput.value.trim() === "") {
				textInput.focus();
				return true;
			}
		}

		return false;
	};

	const hasInvalidConfigurationTextFields = () => {
		const editableTextInputs = junitConfigurationForm.querySelectorAll('input[type="text"]:not([readonly])');
		const allowedValuePattern = /^[A-Za-z0-9._ -]+$/;

		for (const textInput of editableTextInputs) {
			if (!(textInput instanceof HTMLInputElement)) {
				continue;
			}

			if (!allowedValuePattern.test(textInput.value.trim())) {
				textInput.focus();
				return true;
			}
		}

		return false;
	};

	const hasNumericCharactersInRestrictedFields = () => {
		if (/\d/.test(projectNameInput.value)) {
			projectNameInput.focus();
			return true;
		}

		return false;
	};

	const preventNumericCharacters = (textInput) => {
		textInput.addEventListener("input", () => {
			const sanitizedValue = textInput.value.replaceAll(/\d+/g, "");

			if (sanitizedValue !== textInput.value) {
				textInput.value = sanitizedValue;
			}
		});
	};

	const hasInvalidGroupIdFormat = () => {
		const validGroupIdPattern = /^[A-Za-z]+\.[A-Za-z]+$/;

		if (!validGroupIdPattern.test(groupIdInput.value.trim())) {
			groupIdInput.focus();
			return true;
		}

		return false;
	};

	const sanitizeGroupIdInput = () => {
		groupIdInput.addEventListener("input", () => {
			const lettersAndDotsOnly = groupIdInput.value.replaceAll(/[^A-Za-z.]+/g, "");
			const singleDotValue = lettersAndDotsOnly
				.replaceAll(/\.{2,}/g, ".")
				.replaceAll(/\.(?=.*\.)/g, "");

			if (singleDotValue !== groupIdInput.value) {
				groupIdInput.value = singleDotValue;
			}
		});
	};

	const hideValidationAlert = () => {
		validationAlertOverlay.classList.add("d-none");
		validationAlertOverlay.setAttribute("aria-hidden", "true");
	};

	const showValidationAlert = (message) => {
		validationAlertMessage.textContent = message;
		validationAlertOverlay.classList.remove("d-none");
		validationAlertOverlay.setAttribute("aria-hidden", "false");
		validationAlertConfirmBtn.focus();
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

	const getDisplayProjectName = (projectPath) => projectPath.split(/[\\/]/).findLast(Boolean) ?? projectPath;

	const showGeneratedProject = (projectName) => {
		const encodedProjectName = encodeURIComponent(projectName);
		const displayProjectName = getDisplayProjectName(projectName);
		renderGeneratedProjectState(`
			<p class="generated-project-text mb-0">Project created successfully.</p>
			<p class="generated-project-name mb-0">${displayProjectName}</p>
			<div class="generated-project-actions">
				<a class="btn generated-project-button generated-project-button-download" href="/newProject/download?projectName=${encodedProjectName}">Download</a>
				<button type="button" class="btn generated-project-button generated-project-button-create-git" data-project-name="${projectName}">Create Git Project</button>
				<button type="button" class="btn generated-project-button generated-project-button-delete" data-project-name="${projectName}">Delete</button>
			</div>
		`);
	};

	const openCreateGitProjectModal = (projectName) => {
		createGitProjectName.value = getDisplayProjectName(projectName);
		const modalInstance = globalThis.bootstrap?.Modal.getOrCreateInstance(createGitProjectModal);
		modalInstance?.show();
	};

	const handleDeleteProject = async (projectName) => {
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
	};

	mavenButton?.addEventListener("click", () => {
		selectedPackageManager = "maven";
		updateGenerateButtons();
	});

	gradleButton?.addEventListener("click", () => {
		selectedPackageManager = "gradle";
		updateGenerateButtons();
	});

	preventNumericCharacters(projectNameInput);
	sanitizeGroupIdInput();

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

		if (hasEmptyConfigurationTextFields()) {
			showValidationAlert("All fields must be filled in.");
			return;
		}

		if (hasInvalidConfigurationTextFields()) {
			showValidationAlert("Special characters are not allowed in text fields.");
			return;
		}

		if (hasNumericCharactersInRestrictedFields()) {
			showValidationAlert("Project Name cannot contain numeric characters.");
			return;
		}

		if (hasInvalidGroupIdFormat()) {
			showValidationAlert("Group Id must use the format stringa1.stringa2 with letters only.");
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
	validationAlertCloseBtn.addEventListener("click", hideValidationAlert);
	validationAlertConfirmBtn.addEventListener("click", hideValidationAlert);
	validationAlertOverlay.addEventListener("click", (event) => {
		if (event.target === validationAlertOverlay) {
			hideValidationAlert();
		}
	});
	globalThis.addEventListener("keydown", (event) => {
		if (event.key === "Escape" && !validationAlertOverlay.classList.contains("d-none")) {
			hideValidationAlert();
		}
	});
	generatedProjectPanel.addEventListener("click", async (event) => {
		const target = event.target;
		if (!(target instanceof HTMLElement)) {
			return;
		}

		const createGitButton = target.closest(".generated-project-button-create-git");
		if (createGitButton instanceof HTMLButtonElement) {
			const projectName = createGitButton.dataset.projectName;
			if (!projectName) {
				return;
			}

			openCreateGitProjectModal(projectName);
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

		await handleDeleteProject(projectName);
	});

	junitConfigurationModal?.addEventListener("show.bs.modal", updateGenerateButtons);
	updateGenerateButtons();
})();
