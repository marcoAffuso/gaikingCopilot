(function () {
	const modelInput = document.getElementById("model");
	const junitConfigurationModal = document.getElementById("junitConfigurationModal");
	const modelSelectionModal = document.getElementById("modelSelectionModal");
	const modelSelectionStatus = document.getElementById("modelSelectionStatus");
	const modelSelectionList = document.getElementById("modelSelectionList");
	let modelsLoaded = false;
	let reopenJunitConfigurationModal = false;

	if (!modelInput || !junitConfigurationModal || !modelSelectionModal || !modelSelectionStatus || !modelSelectionList) {
		return;
	}

	const renderModelList = (models) => {
		if (!Array.isArray(models) || models.length === 0) {
			modelSelectionStatus.textContent = "No Copilot models available.";
			modelSelectionStatus.classList.remove("d-none");
			modelSelectionList.classList.add("d-none");
			modelSelectionList.innerHTML = "";
			return;
		}

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

	modelSelectionModal.addEventListener("hidden.bs.modal", () => {
		if (!reopenJunitConfigurationModal) {
			return;
		}

		reopenJunitConfigurationModal = false;
		const junitModalInstance = globalThis.bootstrap?.Modal.getOrCreateInstance(junitConfigurationModal);
		junitModalInstance?.show();
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

		modelInput.value = selectionButton.dataset.modelValue ?? "";
		reopenJunitConfigurationModal = true;

		const bootstrapModal = globalThis.bootstrap?.Modal.getInstance(modelSelectionModal);
		bootstrapModal?.hide();
	});
})();
