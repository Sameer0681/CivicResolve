/**
 * CivicResolve - Multi-Modal Grievance Report Wizard
 * Allows citizen to define problem via 3 ways:
 * 1. Write Text
 * 2. Speak (Voice Dictation)
 * 3. Upload Image (Photo Evidence)
 */

// ── State Management ──
const PipelineState = {
    problemMode: "write", // "write", "speak", or "image"
    title: "",
    description: "",
    sector: "Roads & Transport",
    coords: {
        lat: null,
        lng: null,
        accuracy: null
    },
    address: "",
    isSubmitting: false
};

// Civic service categories accepted by the complaint form.
const SECTORS_REGISTRY = [
    "Roads & Transport",
    "Sanitation & Waste",
    "Water Supply",
    "Street Lighting",
    "Electricity",
    "Healthcare",
    "Education",
    "Public Safety",
    "Municipal Services",
    "Revenue & Certificates",
    "Social Welfare",
    "Environment",
    "Housing",
    "Agriculture"
];

let leafletMap = null;
let leafletMarker = null;
let leafletCircle = null;
let leafletPin = null;

// ── DOM Initialization ──
document.addEventListener('DOMContentLoaded', () => {
    initTicketBadge();
    populateSectorSelect();
    checkUrlParams();
    initLeafletMap();
    detectDeviceGPS();
    initSpeechRecognition();
    initMediaUpload();
    initValidationListeners();
    initOtpModalListeners();
});

function initTicketBadge() {
    const badge = document.getElementById('docketTicketIdBadge');
    if (badge) badge.textContent = "Assigned after submission";
}

function checkUrlParams() {
    const params = new URLSearchParams(window.location.search);
    const secParam = params.get('sector');
    if (secParam) {
        const found = SECTORS_REGISTRY.find(s => s.toLowerCase().includes(secParam.toLowerCase()));
        if (found) setSector(found);
    }
}

/**
 * ── 3 WAYS TO DEFINE PROBLEM SWITCHER ──
 */
function switchProblemMode(mode) {
    PipelineState.problemMode = mode;

    ['write', 'speak', 'image'].forEach(m => {
        const tabBtn = document.getElementById(`tabBtn_${m}`);
        const panel = document.getElementById(`modePanel_${m}`);
        if (tabBtn) tabBtn.classList.toggle('active', m === mode);
        if (panel) panel.classList.toggle('active', m === mode);
    });
}

function populateSectorSelect() {
    const sel = document.getElementById('formSectorSelect');
    if (!sel) return;

    sel.innerHTML = '';
    SECTORS_REGISTRY.forEach(name => {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        if (name === PipelineState.sector) opt.selected = true;
        sel.appendChild(opt);
    });

    sel.addEventListener('change', (e) => {
        setSector(e.target.value);
    });
}

function setSector(sectorName) {
    PipelineState.sector = sectorName;
    const sel = document.getElementById('formSectorSelect');
    if (sel && sel.value !== sectorName) sel.value = sectorName;
}

/**
 * Clean Interactive Leaflet Map & GPS
 */
function initLeafletMap() {
    const mapContainer = document.getElementById('locationPinMap');
    if (!mapContainer || typeof L === 'undefined') return;

    if (!leafletMap) {
        leafletMap = L.map('locationPinMap', {
            zoomControl: true,
            attributionControl: false
        }).setView([0, 0], 2);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(leafletMap);

        leafletPin = L.divIcon({
            className: 'cr-clean-pin',
            html: '<div style="background:#2563eb; color:#fff; width:32px; height:32px; border-radius:50% 50% 50% 0; transform:rotate(-45deg); display:flex; align-items:center; justify-content:center; border:2px solid #fff; box-shadow:0 3px 10px rgba(37,99,235,0.4);"><i class="fa-solid fa-location-dot" style="transform:rotate(45deg); font-size:14px;"></i></div>',
            iconSize: [32, 32],
            iconAnchor: [16, 32]
        });

        leafletMap.on('click', (e) => {
            updateCoords(e.latlng.lat, e.latlng.lng, 5.0, true);
        });
    }
}

function updateCoords(lat, lng, accuracy = 8.0, doReverse = true) {
    PipelineState.coords.lat = Number(lat.toFixed(5));
    PipelineState.coords.lng = Number(lng.toFixed(5));
    PipelineState.coords.accuracy = Number(accuracy.toFixed(1));

    if (leafletMap && !leafletMarker && leafletPin) {
        leafletMarker = L.marker([lat, lng], { draggable: true, icon: leafletPin }).addTo(leafletMap);
        leafletMarker.on('dragend', (e) => {
            const p = e.target.getLatLng();
            updateCoords(p.lat, p.lng, 5.0, true);
        });
    } else if (leafletMarker) {
        leafletMarker.setLatLng([lat, lng]);
    }
    if (leafletMap && !leafletCircle) {
        leafletCircle = L.circle([lat, lng], {
            radius: accuracy,
            color: '#2563eb',
            fillColor: '#60a5fa',
            fillOpacity: 0.15,
            weight: 1.5
        }).addTo(leafletMap);
    } else if (leafletCircle) {
        leafletCircle.setLatLng([lat, lng]);
        leafletCircle.setRadius(accuracy);
    }
    if (leafletMap) leafletMap.setView([lat, lng], Math.max(leafletMap.getZoom(), 15));

    const pill = document.getElementById('mapCoordsPill');
    if (pill) {
        pill.innerHTML = `<i class="fa-solid fa-location-crosshairs text-primary me-1"></i> ${PipelineState.coords.lat}, ${PipelineState.coords.lng}`;
    }

    if (doReverse) reverseGeocode(lat, lng);
}

function detectDeviceGPS() {
    const refreshIcon = document.getElementById('gpsRefreshIcon');
    if (refreshIcon) refreshIcon.classList.add('fa-spin');

    if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                if (refreshIcon) refreshIcon.classList.remove('fa-spin');
                updateCoords(pos.coords.latitude, pos.coords.longitude, pos.coords.accuracy || 7.5, true);
            },
            () => {
                if (refreshIcon) refreshIcon.classList.remove('fa-spin');
                showLocationHint("Location unavailable. Enter an address or select a point on the map.");
            },
            { enableHighAccuracy: true, timeout: 7000, maximumAge: 0 }
        );
    } else {
        if (refreshIcon) refreshIcon.classList.remove('fa-spin');
        showLocationHint("Location is not supported by this browser. Enter an address or select a point on the map.");
    }
}

function showLocationHint(message) {
    const pill = document.getElementById('mapCoordsPill');
    if (pill) {
        pill.innerHTML = `<i class="fa-solid fa-location-crosshairs text-primary me-1"></i> ${message}`;
    }
}

function reverseGeocode(lat, lng) {
    const addrInput = document.getElementById('formLocationInput');

    fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`, {
        headers: { 'Accept-Language': 'en' }
    })
    .then(r => r.json())
    .then(data => {
        if (data && data.display_name) {
            const shortAddr = data.display_name.split(',').slice(0, 4).join(', ');
            PipelineState.address = shortAddr;
            if (addrInput) addrInput.value = shortAddr;
        } else {
            fallbackAddr(lat, lng);
        }
    })
    .catch(() => fallbackAddr(lat, lng));
}

function fallbackAddr(lat, lng) {
    showLocationHint(`Address lookup unavailable. Coordinates selected: ${lat.toFixed(5)}, ${lng.toFixed(5)}.`);
}

/**
 * ── WAY 2: VOICE DICTATION LOGIC ──
 */
let isMicActive = false;
let recognizer = null;

function initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const micBtn = document.getElementById('voiceMicBtn');
    const wave = document.getElementById('voiceWaveBar');
    const statusBadge = document.getElementById('voiceStatusBadge');
    const transcriptEl = document.getElementById('voiceTranscriptText');
    const titleEl = document.getElementById('voiceTitleText');
    const helpEl = document.getElementById('voiceHelpText');

    if (!micBtn) return;
    if (!SpeechRecognition) {
        if (transcriptEl) transcriptEl.textContent = "Speech recognition is not supported in this browser. Please use Write or Upload Image.";
        return;
    }

    try {
        recognizer = new SpeechRecognition();
        recognizer.continuous = true;
        recognizer.interimResults = true;
        recognizer.lang = 'en-IN';

        recognizer.onstart = () => {
            isMicActive = true;
            micBtn.classList.add('recording');
            if (wave) wave.style.display = 'flex';
            if (statusBadge) statusBadge.style.display = 'inline-block';
            if (titleEl) titleEl.textContent = "Listening... Speak Now";
            if (helpEl) helpEl.textContent = "Say what the problem is and where it is located.";
        };

        recognizer.onresult = (ev) => {
            let fullText = '';
            for (let i = 0; i < ev.results.length; i++) {
                fullText += ev.results[i][0].transcript;
            }
            if (transcriptEl) transcriptEl.textContent = `"${fullText}"`;

            // Auto-populate form fields
            const formTitle = document.getElementById('formTitleInput');
            const formDesc = document.getElementById('formDescInput');

            if (formTitle) {
                let shortTitle = fullText;
                if (shortTitle.length > 75) shortTitle = shortTitle.substring(0, 72) + "...";
                formTitle.value = shortTitle.charAt(0).toUpperCase() + shortTitle.slice(1);
                formTitle.dispatchEvent(new Event('input'));
            }
            if (formDesc) {
                formDesc.value = fullText;
                formDesc.dispatchEvent(new Event('input'));
            }
        };

        recognizer.onend = () => {
            isMicActive = false;
            micBtn.classList.remove('recording');
            if (wave) wave.style.display = 'none';
            if (statusBadge) statusBadge.style.display = 'none';
            if (titleEl) titleEl.textContent = "Voice Recorded Successfully";
            if (helpEl) helpEl.textContent = "Tap again to re-record or adjust text if needed.";
        };

        micBtn.addEventListener('click', () => {
            if (!isMicActive) recognizer.start();
            else recognizer.stop();
        });

    } catch (e) {
        console.warn("Speech recognition notice:", e);
    }
}

/**
 * ── WAY 3: PHOTO EVIDENCE UPLOAD LOGIC ──
 */
function initMediaUpload() {
    const fileInput = document.getElementById('evidenceFileInput');
    const dropArea = document.getElementById('evidenceDropArea');
    const previewBox = document.getElementById('photoPreviewBox');
    const previewImg = document.getElementById('photoPreviewImg');
    const fileMeta = document.getElementById('photoMetaText');
    const removeBtn = document.getElementById('photoRemoveBtn');
    const captionInput = document.getElementById('photoCaptionInput');
    let lastCaption = '';

    if (!fileInput || !dropArea) return;

    ['dragenter', 'dragover'].forEach(n => {
        dropArea.addEventListener(n, (e) => { e.preventDefault(); dropArea.style.borderColor = '#2563eb'; });
    });
    ['dragleave', 'drop'].forEach(n => {
        dropArea.addEventListener(n, (e) => { e.preventDefault(); dropArea.style.borderColor = '#cbd5e1'; });
    });

    dropArea.addEventListener('drop', (e) => {
        if (e.dataTransfer.files && e.dataTransfer.files[0]) handleFile(e.dataTransfer.files[0]);
    });
    fileInput.addEventListener('change', () => {
        if (fileInput.files && fileInput.files[0]) handleFile(fileInput.files[0]);
    });

    if (captionInput) {
        captionInput.addEventListener('input', () => {
            const formTitle = document.getElementById('formTitleInput');
            const formDesc = document.getElementById('formDescInput');
            const caption = captionInput.value.trim();
            if (formTitle && (!formTitle.value || formTitle.value === lastCaption)) {
                formTitle.value = caption;
                formTitle.dispatchEvent(new Event('input'));
            }
            if (formDesc && (!formDesc.value || formDesc.value === lastCaption)) {
                formDesc.value = caption;
                formDesc.dispatchEvent(new Event('input'));
            }
            lastCaption = caption;
        });
    }

    if (removeBtn) {
        removeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            fileInput.value = '';
            if (previewBox) previewBox.style.display = 'none';
            if (dropArea) dropArea.style.display = 'block';
        });
    }

    function handleFile(file) {
        if (file.size > 10 * 1024 * 1024) {
            alert("File size exceeds 10MB limit.");
            return;
        }
        const reader = new FileReader();
        reader.onload = (e) => {
            if (previewImg) previewImg.src = e.target.result;
            if (fileMeta) fileMeta.textContent = `${file.name} (${(file.size / 1024).toFixed(0)} KB)`;
            dropArea.style.display = 'none';
            if (previewBox) previewBox.style.display = 'flex';
        };
        reader.readAsDataURL(file);
    }
}

/**
 * Input Listeners & Live Character Counters
 */
function initValidationListeners() {
    const title = document.getElementById('formTitleInput');
    const desc = document.getElementById('formDescInput');
    const loc = document.getElementById('formLocationInput');

    if (title) {
        title.addEventListener('input', () => {
            PipelineState.title = title.value.trim();
            const counter = document.getElementById('titleCharCount');
            if (counter) counter.textContent = `${title.value.length}/150`;

            const valid = title.value.length >= 5 && title.value.length <= 150;
            title.classList.toggle('is-valid', valid);
            title.classList.toggle('is-invalid', !valid && title.value.length > 0);
        });
    }

    if (desc) {
        desc.addEventListener('input', () => {
            PipelineState.description = desc.value.trim();
            const counter = document.getElementById('descCharCount');
            if (counter) counter.textContent = `${desc.value.length}/1000`;

            const valid = desc.value.length >= 10;
            desc.classList.toggle('is-valid', valid);
            desc.classList.toggle('is-invalid', !valid && desc.value.length > 0);
        });
    }

    if (loc) {
        loc.addEventListener('input', () => {
            PipelineState.address = loc.value.trim();
            const valid = loc.value.length >= 5;
            loc.classList.toggle('is-valid', valid);
            loc.classList.toggle('is-invalid', !valid && loc.value.length > 0);
        });
    }
}

let otpResendInterval = null;
let cachedComplaintPayload = null;
let cachedComplaintContext = null;

function initOtpModalListeners() {
    const verifyBtn = document.getElementById('otpVerifyBtn');
    const resendBtn = document.getElementById('otpResendBtn');
    const inputEl = document.getElementById('otpCodeInput');

    if (verifyBtn) {
        verifyBtn.addEventListener('click', verifyOtpCode);
    }

    if (resendBtn) {
        resendBtn.addEventListener('click', () => {
            hideOtpModal();
            // Resend complaint submission without token to trigger fresh OTP
            submitGrievanceAsync(null);
        });
    }

    if (inputEl) {
        inputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                verifyOtpCode();
            }
        });
        inputEl.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/\D/g, '').substring(0, 6);
        });
    }
}

function showOtpModal(data) {
    const modalEl = document.getElementById('otpVerificationModal');
    if (!modalEl) {
        alert(data.message || "OTP verification is required. Please check your phone.");
        return;
    }

    const phoneEl = document.getElementById('otpMaskedPhoneText');
    if (phoneEl) {
        phoneEl.textContent = data.maskedPhone || 'your registered phone';
    }

    const inputEl = document.getElementById('otpCodeInput');
    if (inputEl) {
        inputEl.value = '';
        inputEl.classList.remove('is-invalid');
    }

    const errorEl = document.getElementById('otpErrorMessage');
    if (errorEl) {
        errorEl.classList.add('d-none');
        errorEl.textContent = '';
    }

    startOtpResendTimer(data.resendAfterSeconds || 60);

    if (window.bootstrap && bootstrap.Modal) {
        const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
        bsModal.show();
    } else {
        modalEl.classList.add('show');
        modalEl.style.display = 'block';
    }

    setTimeout(() => {
        if (inputEl) inputEl.focus();
    }, 350);
}

function hideOtpModal() {
    const modalEl = document.getElementById('otpVerificationModal');
    if (modalEl) {
        if (window.bootstrap && bootstrap.Modal) {
            const bsModal = bootstrap.Modal.getInstance(modalEl);
            if (bsModal) bsModal.hide();
        } else {
            modalEl.classList.remove('show');
            modalEl.style.display = 'none';
        }
    }
    if (otpResendInterval) {
        clearInterval(otpResendInterval);
        otpResendInterval = null;
    }
}

function startOtpResendTimer(seconds) {
    if (otpResendInterval) clearInterval(otpResendInterval);

    const resendBtn = document.getElementById('otpResendBtn');
    const timerText = document.getElementById('otpTimerText');
    if (!resendBtn) return;

    let remaining = parseInt(seconds, 10);
    if (isNaN(remaining) || remaining < 0) remaining = 60;

    resendBtn.disabled = true;

    function render() {
        if (remaining <= 0) {
            clearInterval(otpResendInterval);
            otpResendInterval = null;
            resendBtn.disabled = false;
            if (timerText) timerText.textContent = '';
        } else {
            if (timerText) timerText.textContent = `(${remaining}s)`;
            remaining--;
        }
    }

    render();
    otpResendInterval = setInterval(render, 1000);
}

async function verifyOtpCode() {
    const inputEl = document.getElementById('otpCodeInput');
    const errorEl = document.getElementById('otpErrorMessage');
    const verifyBtn = document.getElementById('otpVerifyBtn');
    const spinner = document.getElementById('otpVerifySpinner');

    const code = inputEl ? inputEl.value.trim() : '';
    if (!code || code.length !== 6 || !/^\d{6}$/.test(code)) {
        if (errorEl) {
            errorEl.textContent = "Please enter a valid 6-digit verification code.";
            errorEl.classList.remove('d-none');
        }
        if (inputEl) {
            inputEl.classList.add('is-invalid');
            inputEl.focus();
        }
        return;
    }

    if (errorEl) errorEl.classList.add('d-none');
    if (inputEl) inputEl.classList.remove('is-invalid');
    if (verifyBtn) verifyBtn.disabled = true;
    if (spinner) spinner.classList.remove('d-none');

    try {
        const response = await fetch('/api/otp/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ otp: code })
        });

        const data = await response.json().catch(() => ({}));

        if (response.ok && data.verified && data.otpToken) {
            hideOtpModal();
            // Re-submit same complaint payload with X-OTP-Token
            await submitGrievanceAsync(data.otpToken);
        } else {
            const msg = data.message || "Invalid or expired verification code. Please check and try again.";
            if (errorEl) {
                errorEl.textContent = msg;
                errorEl.classList.remove('d-none');
            }
            if (inputEl) {
                inputEl.classList.add('is-invalid');
                inputEl.select();
            }
        }
    } catch (e) {
        if (errorEl) {
            errorEl.textContent = "Network error verifying code. Please try again.";
            errorEl.classList.remove('d-none');
        }
    } finally {
        if (verifyBtn) verifyBtn.disabled = false;
        if (spinner) spinner.classList.add('d-none');
    }
}

/**
 * Asynchronous Submission to POST /api/complaints
 */
async function submitGrievanceAsync(otpToken = null) {
    if (PipelineState.isSubmitting) return;

    let titleVal = document.getElementById('formTitleInput')?.value.trim() || PipelineState.title;
    let descVal = document.getElementById('formDescInput')?.value.trim() || PipelineState.description;
    const locVal = document.getElementById('formLocationInput')?.value.trim() || PipelineState.address;
    const secVal = document.getElementById('formSectorSelect')?.value || PipelineState.sector;

    // Fallbacks based on problem mode
    if (PipelineState.problemMode === 'speak') {
        const transcript = document.getElementById('voiceTranscriptText')?.textContent.trim();
        if ((!titleVal || titleVal.length < 5) && transcript && transcript.length >= 5) {
            titleVal = transcript.substring(0, 70);
            descVal = transcript;
        }
    } else if (PipelineState.problemMode === 'image') {
        const caption = document.getElementById('photoCaptionInput')?.value.trim();
        if (!titleVal || titleVal.length < 5) {
            titleVal = caption || "";
        }
        if (!descVal || descVal.length < 10) {
            descVal = caption || "";
        }
    }

    if (!titleVal || titleVal.length < 5) {
        alert("Please define your complaint title (at least 5 characters).");
        document.getElementById('formTitleInput')?.focus();
        return;
    }

    if (!descVal || descVal.length < 10) {
        alert("Please provide a description of at least 10 characters.");
        document.getElementById('formDescInput')?.focus();
        return;
    }

    if (!locVal || locVal.length < 3) {
        alert("Please specify the incident location or landmark.");
        document.getElementById('formLocationInput')?.focus();
        return;
    }

    PipelineState.isSubmitting = true;

    const overlay = document.getElementById('restLoadingOverlay');
    const statusText = document.getElementById('restLoadingStatus');
    if (overlay) overlay.style.display = 'flex';
    if (statusText) statusText.textContent = "Registering ticket & verifying with authority desk...";

    const complaintPayload = {
        title: titleVal,
        description: descVal,
        sector: secVal,
        location: PipelineState.coords.lat !== null && PipelineState.coords.lng !== null
            ? `${locVal} [${PipelineState.coords.lat}, ${PipelineState.coords.lng}]`
            : locVal,
        status: "SUBMITTED"
    };

    cachedComplaintPayload = complaintPayload;
    cachedComplaintContext = { titleVal, descVal, locVal, secVal };

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    if (otpToken) {
        headers['X-OTP-Token'] = otpToken;
    }

    try {
        if (statusText) {
            statusText.textContent = "Submitting complaint securely...";
        }

        const response = await fetch('/api/complaints', {
            method: 'POST',
            headers: headers,
            credentials: 'same-origin',
            body: JSON.stringify(complaintPayload)
        });

        // Form-login security may redirect unauthenticated requests to /login.
        if (response.redirected || (response.url && response.url.includes('/login'))) {
            window.location.href =
                `/login?returnUrl=${encodeURIComponent('/report')}`;
            return;
        }

        // 1. Success (200 OK)
        if (response.ok) {
            const savedComplaint = await response.json();

            hideOtpModal();

            const savedId = savedComplaint.complaintId;
            if (!savedId) {
                alert("The complaint was saved, but the server did not return its assigned ID. Do not submit again; contact support for the receipt.");
                return;
            }
            const redirectUrl = `/report/submission-success?complaintId=${encodeURIComponent(savedId)}&title=${encodeURIComponent(savedComplaint.title || titleVal)}&description=${encodeURIComponent(savedComplaint.description || descVal)}&sector=${encodeURIComponent(savedComplaint.sector || secVal)}&location=${encodeURIComponent(savedComplaint.location || complaintPayload.location)}&urgency=${encodeURIComponent(savedComplaint.urgency || "")}&dept=${encodeURIComponent(savedComplaint.department || "")}`;

            window.location.href = redirectUrl;
            return;
        }

        // 2. OTP Required (HTTP 428)
        if (response.status === 428) {
            if (overlay) overlay.style.display = 'none';
            const data = await response.json().catch(() => ({}));
            showOtpModal(data);
            return;
        }

        // 3. Error Handling
        if (overlay) overlay.style.display = 'none';
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData.message || "";

        if (response.status === 401 || response.status === 403) {
            alert("You must log in with a Citizen account to submit a complaint.");
            window.location.href =
                `/login?returnUrl=${encodeURIComponent('/report')}`;
            return;
        }

        if (response.status === 400) {
            if (errorMsg.includes("phone") || errorMsg.includes("PUT /api/auth/profile/phone")) {
                alert("Your account does not have a registered mobile phone number. Please add your phone number via profile settings before submitting complaints.");
            } else {
                alert(errorMsg || "Unable to submit grievance due to invalid input. Please check your details.");
            }
            return;
        }

        if (response.status === 409) {
            alert(errorMsg || "This verification code or ticket has already been processed. Please request a new verification code.");
            return;
        }

        if (response.status === 429) {
            alert("Too many OTP requests have been sent recently. Please wait a few minutes before trying again.");
            return;
        }

        alert(errorMsg || `Server error (${response.status}). Please try again later.`);

    } catch (err) {
        if (overlay) overlay.style.display = 'none';
        console.error("Complaint submission failed:", err);
        alert(
            "Your complaint could not be submitted. No success receipt was created. " +
            "Please log in as a Citizen if required, then try again."
        );
    } finally {
        PipelineState.isSubmitting = false;
        if (overlay) overlay.style.display = 'none';
    }
}
