(function () {
	const modelInput = document.getElementById("model");
	const reasoningEffortSelect = document.getElementById("reasoningEffort");
	const getModelLlmBtn = document.getElementById("getModelLlmBtn");
	const generateProjectMavenBtn = document.getElementById("generateProjectMavenBtn");
	const generateProjectGradleBtn = document.getElementById("generateProjectGradleBtn");
	const generateConfluenceTestCaseBtn = document.getElementById("generateConfluenceTestCaseBtn");
	const junitConfigurationModal = document.getElementById("junitConfigurationModal");
	const confluenceConfigurationModal = document.getElementById("confluenceConfigurationModal");
	const modelSelectionModal = document.getElementById("modelSelectionModal");
	const modelSelectionStatus = document.getElementById("modelSelectionStatus");
	const modelSelectionList = document.getElementById("modelSelectionList");
	const modelsById = new Map();
	const primaryConfigurationModal = junitConfigurationModal ?? confluenceConfigurationModal;
	let modelsLoaded = false;
	let reopenConfigurationModal = false;
	let preserveReasoningEffortsOnNextConfigurationHide = false;

	if (!modelInput || !reasoningEffortSelect || !getModelLlmBtn || !primaryConfigurationModal || !modelSelectionModal || !modelSelectionStatus || !modelSelectionList) {
		return;
	}

	const resetReasoningEfforts = () => {
		reasoningEffortSelect.innerHTML = '<option value="NA" selected>NA</option>';
		reasoningEffortSelect.disabled = true;
	};

	const populateReasoningEfforts = (model) => {
		const hasReasoningSupport = Boolean(model?.reasoningEffort ?? model?.isReasoningEffort);
		const supportedReasoningEfforts = Array.isArray(model?.supportedReasoningEfforts)
			? model.supportedReasoningEfforts.filter((effort) => typeof effort === "string" && effort.trim() !== "")
			: [];

		if (!hasReasoningSupport || supportedReasoningEfforts.length === 0) {
			resetReasoningEfforts();
			return;
		}

		reasoningEffortSelect.innerHTML = supportedReasoningEfforts
			.map((effort, index) => `<option value="${effort}"${index === 0 ? " selected" : ""}>${effort}</option>`)
			.join("");
		reasoningEffortSelect.disabled = false;
	};

	resetReasoningEfforts();

	const renderModelList = (models) => {
		if (!Array.isArray(models) || models.length === 0) {
			modelsById.clear();
			resetReasoningEfforts();
			modelSelectionStatus.textContent = "No Copilot models available.";
			modelSelectionStatus.classList.remove("d-none");
			modelSelectionList.classList.add("d-none");
			modelSelectionList.innerHTML = "";
			return;
		}

		modelsById.clear();
		models.forEach((model) => {
			if (model?.id) {
				modelsById.set(model.id, model);
			}
		});

		modelSelectionList.innerHTML = models
			.map((model) => {
				const reasoningEfforts = Array.isArray(model.supportedReasoningEfforts) && model.supportedReasoningEfforts.length > 0
					? model.supportedReasoningEfforts.join(", ")
					: "Not available";
				const reasoningSupport = model.reasoningEffort ?? model.isReasoningEffort;
				return `
					<div class="model-selection-item">
						<div class="model-selection-item-header">
							<div>
								<h3 class="model-selection-item-title">${model.name ?? model.id ?? "Unknown model"}</h3>
								<p class="model-selection-item-subtitle">${model.id ?? "No id available"}</p>
							</div>
							<button type="button" class="btn model-selection-button" data-model-value="${model.id ?? ""}">Select</button>
						</div>
						<ul class="model-selection-meta">
							<li><strong>Supported reasoning efforts:</strong> ${reasoningEfforts}</li>
							<li><strong>Reasoning effort support:</strong> ${reasoningSupport ? "Yes" : "No"}</li>
						</ul>
					</div>
				`;
			})
			.join("");

		modelSelectionStatus.classList.add("d-none");
		modelSelectionList.classList.remove("d-none");
	};

	const showModelError = (message) => {
		resetReasoningEfforts();
		modelSelectionList.classList.add("d-none");
		modelSelectionList.innerHTML = "";
		modelSelectionStatus.textContent = message;
		modelSelectionStatus.classList.remove("d-none");
	};

	const loadCopilotModels = async () => {
		if (modelsLoaded) {
			return;
		}

		showModelError("Loading available models...");

		try {
			const response = await fetch("/getCopilotModels", {
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
				throw new Error(`Unable to load models (${response.status})`);
			}

			const models = await response.json();
			renderModelList(models);
			modelsLoaded = true;
		} catch (error) {
			showModelError(error instanceof Error ? error.message : "Unable to load models.");
		}
	};

	modelSelectionModal.addEventListener("show.bs.modal", () => {
		void loadCopilotModels();
	});

	getModelLlmBtn.addEventListener("click", () => {
		preserveReasoningEffortsOnNextConfigurationHide = true;
	});

	primaryConfigurationModal.addEventListener("hidden.bs.modal", () => {
		if (preserveReasoningEffortsOnNextConfigurationHide) {
			preserveReasoningEffortsOnNextConfigurationHide = false;
			return;
		}

		resetReasoningEfforts();
	});

	modelSelectionModal.addEventListener("hidden.bs.modal", () => {
		if (!reopenConfigurationModal) {
			return;
		}

		reopenConfigurationModal = false;
		const configurationModalInstance = globalThis.bootstrap?.Modal.getOrCreateInstance(primaryConfigurationModal);
		configurationModalInstance?.show();
	});

	modelSelectionList.addEventListener("click", (event) => {
		const target = event.target;
		if (!(target instanceof HTMLElement)) {
			return;
		}

		const selectionButton = target.closest(".model-selection-button");
		if (!(selectionButton instanceof HTMLButtonElement)) {
			return;
		}

		const modelId = selectionButton.dataset.modelValue ?? "";
		modelInput.value = modelId;
		populateReasoningEfforts(modelsById.get(modelId));
		reopenConfigurationModal = true;

		const bootstrapModal = globalThis.bootstrap?.Modal.getInstance(modelSelectionModal);
		bootstrapModal?.hide();
	});

	generateProjectMavenBtn?.addEventListener("click", resetReasoningEfforts);
	generateProjectGradleBtn?.addEventListener("click", resetReasoningEfforts);
	generateConfluenceTestCaseBtn?.addEventListener("click", () => {
		preserveReasoningEffortsOnNextConfigurationHide = false;
	});
})();
