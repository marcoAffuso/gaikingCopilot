(function () {
	const confluenceConfigurationModal = document.getElementById("confluenceConfigurationModal");
	const confluenceConfigurationForm = document.getElementById("confluenceConfigurationForm");
	const modelInput = document.getElementById("model");
	const reasoningEffortSelect = document.getElementById("reasoningEffort");
	const confluenceUrlInput = document.getElementById("confluenceUrl");
	const confluenceSpaceInput = document.getElementById("confluenceSpace");
	const confluencePageInput = document.getElementById("confluencePage");
	const outputDirInput = document.getElementById("outputDir");
	const generateConfluenceTestCaseBtn = document.getElementById("generateConfluenceTestCaseBtn");
	const generatedTestCasePanel = document.getElementById("generatedTestCasePanel");
	const generatedTestCaseText = document.getElementById("generatedTestCaseText");

	if (!confluenceConfigurationModal || !confluenceConfigurationForm || !modelInput || !reasoningEffortSelect || !confluenceUrlInput || !confluenceSpaceInput || !confluencePageInput || !outputDirInput || !generateConfluenceTestCaseBtn || !generatedTestCasePanel || !generatedTestCaseText) {
		return;
	}

	const renderGeneratedTestCaseState = (content) => {
		generatedTestCasePanel.innerHTML = content;
	};

	const showGeneratedTestCasePending = () => {
		renderGeneratedTestCaseState(`
			<p class="generated-project-text mb-0">Test case generation in progress...</p>
		`);
	};

	const showGeneratedTestCaseError = (message) => {
		renderGeneratedTestCaseState(`
			<p class="generated-project-text mb-0">${message}</p>
		`);
	};

	const showGeneratedTestCaseSuccess = (message, testCasePath) => {
		renderGeneratedTestCaseState(`
			<p class="generated-project-text mb-0">${message}</p>
			<p class="generated-project-text mb-0">Path: ${testCasePath}</p>
		`);
	};

	const hasEmptyConfluenceFields = () => {
		const textInputs = [modelInput, reasoningEffortSelect, confluenceUrlInput, confluenceSpaceInput, confluencePageInput, outputDirInput];

		for (const textInput of textInputs) {
			if (textInput.value.trim() === "") {
				textInput.focus();
				return true;
			}
		}

		return false;
	};

	const submitConfluenceConfigurationForm = async (event) => {
		event.preventDefault();

		if (hasEmptyConfluenceFields()) {
			showGeneratedTestCaseError("All Confluence fields must be filled in.");
			return;
		}

		showGeneratedTestCasePending();

		try {
			const searchParams = new URLSearchParams({
				model: modelInput.value.trim(),
				reasoningEffort: reasoningEffortSelect.value.trim(),
				confluenceUrl: confluenceUrlInput.value.trim(),
				spaceKey: confluenceSpaceInput.value.trim(),
				pageTitle: confluencePageInput.value.trim(),
				outputDir: outputDirInput.value.trim()
			});

			const response = await fetch(`/generateTestCase/Confluence?${searchParams.toString()}`, {
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
				throw new Error(`Unable to generate test case (${response.status})`);
			}

			const responseBody = await response.json();
			const modalInstance = globalThis.bootstrap?.Modal.getInstance(confluenceConfigurationModal)
				?? globalThis.bootstrap?.Modal.getOrCreateInstance(confluenceConfigurationModal);
			modalInstance?.hide();
			showGeneratedTestCaseSuccess(
				responseBody.message ?? "Test case generated successfully.",
				responseBody.ConfluenceTestCasePath ?? "generateTestCase/Confluence"
			);
		} catch (error) {
			showGeneratedTestCaseError(error instanceof Error ? error.message : "Unable to generate test case.");
		}
	};

	confluenceConfigurationForm.addEventListener("submit", submitConfluenceConfigurationForm);
}());
