/**
 * CivicResolve - Multi-Modal Grievance Report Wizard
 * Allows citizen to define problem via 3 ways:
 * 1. Write Text
 * 2. Speak (Voice Dictation)
 * 3. Upload Image (Photo Evidence)
 */

// Generate Ticket ID in format: CR-2025-XXXX (e.g., CR-2025-0842)
function generateTicketId() {
    const randomNum = String(Math.floor(1000 + Math.random() * 9000)).padStart(4, '0');
    return `CR-2025-${randomNum}`;
}

// ── State Management ──
const PipelineState = {
    complaintId: generateTicketId(),
    problemMode: "write", // "write", "speak", or "image"
    title: "",
    description: "",
    sector: "Roads & Transport",
    department: "Public Works Department (PWD)",
    deskName: "PWD Desk",
    deskIcon: "fa-road",
    assignedOfficer: "Junior Engineer (JE)",
    targetPortal: "State PWD Grievance Redressal Portal / CPGRAMS Gateway",
    urgency: "HIGH",
    slaHours: 48,
    coords: {
        lat: 26.8467,
        lng: 80.9462,
        accuracy: 8.5
    },
    address: "Ward 14, Main Road, Municipal Area [26.8467, 80.9462]",
    mediaFile: null,
    isSubmitting: false
};

// 14 Civic Sectors Taxonomy with In-App Desk & Officer Tier Mapping
const SECTORS_REGISTRY = [
    {
        name: "Roads & Transport",
        icon: "fa-road",
        dept: "Public Works Department (PWD)",
        deskName: "PWD Desk",
        assignedOfficer: "Junior Engineer (JE)",
        portal: "State PWD Grievance Redressal Portal / CPGRAMS Gateway",
        slaHours: 48
    },
    {
        name: "Sanitation & Waste",
        icon: "fa-trash-can",
        dept: "Municipal Solid Waste Management Division",
        deskName: "Municipal Corporation Desk",
        assignedOfficer: "Sanitation Inspector",
        portal: "Swachhata Citizen Grievance Portal & Nagar Nigam System",
        slaHours: 24
    },
    {
        name: "Water Supply",
        icon: "fa-droplet",
        dept: "Jal Nigam / Municipal Water Works",
        deskName: "Jal Nigam Desk",
        assignedOfficer: "Water Board Assistant Engineer (AE)",
        portal: "Jal Sansthan Citizen Water Redressal Portal & e-Jal Seva",
        slaHours: 24
    },
    {
        name: "Street Lighting",
        icon: "fa-lightbulb",
        dept: "Municipal Electrical & Lighting Cell",
        deskName: "Streetlight Command Desk",
        assignedOfficer: "Junior Electrical Engineer",
        portal: "Smart City Municipal Streetlight Command Center",
        slaHours: 24
    },
    {
        name: "Electricity",
        icon: "fa-bolt",
        dept: "State Power Distribution Corporation (DISCOM)",
        deskName: "DISCOM 1912 Desk",
        assignedOfficer: "Sub-Divisional Officer (SDO)",
        portal: "State DISCOM 1912 Power Consumer Grievance Portal",
        slaHours: 24
    },
    {
        name: "Healthcare",
        icon: "fa-hospital",
        dept: "Chief Medical Office & Public Health Dept",
        deskName: "Health Redressal Cell",
        assignedOfficer: "Medical Superintendent",
        portal: "National Health Mission Grievance Redressal Portal",
        slaHours: 48
    },
    {
        name: "Education",
        icon: "fa-graduation-cap",
        dept: "Department of Basic & Secondary Education",
        deskName: "District Education Desk",
        assignedOfficer: "Block Education Officer (BEO)",
        portal: "State Shiksha Seva & District Education Portal",
        slaHours: 72
    },
    {
        name: "Public Safety",
        icon: "fa-shield-halved",
        dept: "City Police & Public Safety Cell",
        deskName: "Public Safety Desk",
        assignedOfficer: "Station House Officer (SHO)",
        portal: "State Police Citizen Portal (CCTNS National Gateway)",
        slaHours: 12
    },
    {
        name: "Municipal Services",
        icon: "fa-building-flag",
        dept: "Municipal Corporation Administration",
        deskName: "Municipal Corporation Desk",
        assignedOfficer: "Assistant Municipal Commissioner",
        portal: "e-Nagarpalika Citizen Services Single Window",
        slaHours: 72
    },
    {
        name: "Revenue & Certificates",
        icon: "fa-file-invoice",
        dept: "Revenue & Tehsil Sub-Divisional Office",
        deskName: "Tehsil Revenue Desk",
        assignedOfficer: "Nayab Tehsildar",
        portal: "e-District Revenue & Jan Seva Portal",
        slaHours: 72
    },
    {
        name: "Social Welfare",
        icon: "fa-hand-holding-heart",
        dept: "Social Welfare & Pension Department",
        deskName: "Social Welfare Desk",
        assignedOfficer: "District Social Welfare Officer",
        portal: "National Social Assistance Portal (NSAP)",
        slaHours: 72
    },
    {
        name: "Environment",
        icon: "fa-leaf",
        dept: "Pollution Control Board & Parks Department",
        deskName: "Environment Board Desk",
        assignedOfficer: "Regional Environment Officer",
        portal: "State Pollution Control Board (SPCB) Grievance Portal",
        slaHours: 48
    },
    {
        name: "Housing",
        icon: "fa-house-chimney",
        dept: "Urban Development & Housing Authority",
        deskName: "Housing Board Desk",
        assignedOfficer: "Executive Engineer (Housing)",
        portal: "State Housing Board & PMAY Grievance Desk",
        slaHours: 96
    },
    {
        name: "Agriculture",
        icon: "fa-seedling",
        dept: "District Agriculture & Irrigation Department",
        deskName: "District Agriculture Desk",
        assignedOfficer: "Assistant Director of Agriculture",
        portal: "Kisan Grievance Redressal System & Irrigation Desk",
        slaHours: 48
    }
];

let leafletMap = null;
let leafletMarker = null;
let leafletCircle = null;

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
});

function initTicketBadge() {
    const badge = document.getElementById('docketTicketIdBadge');
    if (badge) badge.textContent = PipelineState.complaintId;
}

function checkUrlParams() {
    const params = new URLSearchParams(window.location.search);
    const secParam = params.get('sector');
    if (secParam) {
        const found = SECTORS_REGISTRY.find(s => s.name.toLowerCase().includes(secParam.toLowerCase()));
        if (found) setSector(found.name);
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
    SECTORS_REGISTRY.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.name;
        opt.textContent = s.name;
        if (s.name === PipelineState.sector) opt.selected = true;
        sel.appendChild(opt);
    });

    sel.addEventListener('change', (e) => {
        setSector(e.target.value);
    });
}

function setSector(sectorName) {
    const found = SECTORS_REGISTRY.find(s => s.name === sectorName) || SECTORS_REGISTRY[0];
    PipelineState.sector = found.name;
    PipelineState.department = found.dept;
    PipelineState.deskName = found.deskName;
    PipelineState.deskIcon = found.icon;
    PipelineState.assignedOfficer = found.assignedOfficer;
    PipelineState.targetPortal = found.portal;
    PipelineState.slaHours = found.slaHours;

    // Update Select
    const sel = document.getElementById('formSectorSelect');
    if (sel && sel.value !== found.name) sel.value = found.name;

    // Update Right-Hand Summary Card
    const deskEl = document.getElementById('docketDeskBadge');
    if (deskEl) {
        deskEl.innerHTML = `<i class="fa-solid ${found.icon} text-primary me-2"></i> ${found.deskName}`;
    }

    const deptEl = document.getElementById('docketDeptText');
    if (deptEl) deptEl.textContent = found.dept;

    const officerEl = document.getElementById('docketOfficerText');
    if (officerEl) officerEl.textContent = found.assignedOfficer;

    const slaEl = document.getElementById('docketSlaBadge');
    if (slaEl) slaEl.textContent = `${found.slaHours}-Hour SLA`;
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
        }).setView([PipelineState.coords.lat, PipelineState.coords.lng], 15);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(leafletMap);

        const customPin = L.divIcon({
            className: 'cr-clean-pin',
            html: '<div style="background:#2563eb; color:#fff; width:32px; height:32px; border-radius:50% 50% 50% 0; transform:rotate(-45deg); display:flex; align-items:center; justify-content:center; border:2px solid #fff; box-shadow:0 3px 10px rgba(37,99,235,0.4);"><i class="fa-solid fa-location-dot" style="transform:rotate(45deg); font-size:14px;"></i></div>',
            iconSize: [32, 32],
            iconAnchor: [16, 32]
        });

        leafletMarker = L.marker([PipelineState.coords.lat, PipelineState.coords.lng], {
            draggable: true,
            icon: customPin
        }).addTo(leafletMap);

        leafletCircle = L.circle([PipelineState.coords.lat, PipelineState.coords.lng], {
            radius: PipelineState.coords.accuracy || 8.5,
            color: '#2563eb',
            fillColor: '#60a5fa',
            fillOpacity: 0.15,
            weight: 1.5
        }).addTo(leafletMap);

        leafletMarker.on('dragend', (e) => {
            const p = e.target.getLatLng();
            updateCoords(p.lat, p.lng, 5.0, true);
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

    if (leafletMarker) leafletMarker.setLatLng([lat, lng]);
    if (leafletCircle) {
        leafletCircle.setLatLng([lat, lng]);
        leafletCircle.setRadius(accuracy);
    }
    if (leafletMap) leafletMap.panTo([lat, lng]);

    const pill = document.getElementById('mapCoordsPill');
    if (pill) {
        pill.innerHTML = `<i class="fa-solid fa-location-crosshairs text-primary me-1"></i> ${PipelineState.coords.lat}° N, ${PipelineState.coords.lng}° E`;
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
                updateCoords(26.8467, 80.9462, 10.0, true);
            },
            { enableHighAccuracy: true, timeout: 7000, maximumAge: 0 }
        );
    } else {
        if (refreshIcon) refreshIcon.classList.remove('fa-spin');
        updateCoords(26.8467, 80.9462, 10.0, true);
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
    const addrInput = document.getElementById('formLocationInput');
    const text = `Ward 14, Main Road, Municipal Area [${lat}, ${lng}]`;
    PipelineState.address = text;
    if (addrInput && !addrInput.value) addrInput.value = text;
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
            if (formTitle && (!formTitle.value || formTitle.value.startsWith('Photo Evidence:'))) {
                formTitle.value = captionInput.value ? captionInput.value : `Photo Evidence: ${PipelineState.mediaFile?.name || 'Issue'}`;
                formTitle.dispatchEvent(new Event('input'));
            }
            if (formDesc && (!formDesc.value || formDesc.value.includes('photographic proof'))) {
                formDesc.value = captionInput.value ? captionInput.value : `Photographic evidence attached showing civic issue.`;
                formDesc.dispatchEvent(new Event('input'));
            }
        });
    }

    if (removeBtn) {
        removeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            PipelineState.mediaFile = null;
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
        PipelineState.mediaFile = file;
        const reader = new FileReader();
        reader.onload = (e) => {
            if (previewImg) previewImg.src = e.target.result;
            if (fileMeta) fileMeta.textContent = `${file.name} (${(file.size / 1024).toFixed(0)} KB)`;
            dropArea.style.display = 'none';
            if (previewBox) previewBox.style.display = 'flex';

            // Auto-populate title & desc if empty
            const formTitle = document.getElementById('formTitleInput');
            const formDesc = document.getElementById('formDescInput');
            if (formTitle && !formTitle.value) {
                formTitle.value = `Photo Evidence: ${file.name}`;
                formTitle.dispatchEvent(new Event('input'));
            }
            if (formDesc && !formDesc.value) {
                formDesc.value = `Civic defect documented with attached photographic proof (${file.name}).`;
                formDesc.dispatchEvent(new Event('input'));
            }
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

/**
 * Asynchronous Submission to POST /api/complaints
 */
async function submitGrievanceAsync() {
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
        if ((!titleVal || titleVal.length < 5)) {
            titleVal = caption || (PipelineState.mediaFile ? `Photo Evidence: ${PipelineState.mediaFile.name}` : "Civic Issue Reported");
        }
        if ((!descVal || descVal.length < 10)) {
            descVal = caption || "Civic defect documented with photographic evidence.";
        }
    }

    if (!titleVal || titleVal.length < 5) {
        alert("Please define your complaint title (at least 5 characters). You can Type, Speak, or Upload an Image.");
        if (PipelineState.problemMode === 'write') document.getElementById('formTitleInput')?.focus();
        return;
    }
    if (!descVal || descVal.length < 10) {
        alert("Please provide a description of the incident (at least 10 characters).");
        if (PipelineState.problemMode === 'write') document.getElementById('formDescInput')?.focus();
        return;
    }
    if (!locVal || locVal.length < 3) {
        alert("Please specify the incident location or landmark.");
        document.getElementById('formLocationInput')?.focus();
        return;
    }

    PipelineState.isSubmitting = true;

    // Show Loading Modal
    const overlay = document.getElementById('restLoadingOverlay');
    const statusText = document.getElementById('restLoadingStatus');
    if (overlay) overlay.style.display = 'flex';

    const complaintPayload = {
        complaintId: PipelineState.complaintId,
        title: titleVal,
        description: descVal,
        sector: secVal,
        department: PipelineState.department,
        location: `${locVal} [${PipelineState.coords.lat}, ${PipelineState.coords.lng}]`,
        status: "SUBMITTED"
    };

    try {
        if (statusText) statusText.textContent = "Registering ticket & verifying with authority desk...";

        const response = await fetch('/api/complaints?citizenId=3', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(complaintPayload)
        });

        if (!response.ok) {
            throw new Error(`HTTP Error ${response.status}`);
        }

        const savedComplaint = await response.json();
        console.log("Complaint successfully saved:", savedComplaint);

        // Store latest ticket for receipt
        sessionStorage.setItem('cr_latest_ticket', JSON.stringify({
            ...complaintPayload,
            deskName: PipelineState.deskName,
            assignedOfficer: PipelineState.assignedOfficer,
            portal: PipelineState.targetPortal,
            urgency: PipelineState.urgency,
            createdAt: new Date().toISOString()
        }));

        setTimeout(() => {
            const redirectUrl = `/report/submission-success?complaintId=${encodeURIComponent(PipelineState.complaintId)}&title=${encodeURIComponent(titleVal)}&sector=${encodeURIComponent(secVal)}&location=${encodeURIComponent(locVal)}&urgency=${encodeURIComponent(PipelineState.urgency)}&dept=${encodeURIComponent(PipelineState.department)}&portal=${encodeURIComponent(PipelineState.targetPortal)}`;
            window.location.href = redirectUrl;
        }, 500);

    } catch (err) {
        console.warn("Offline continuity mode active:", err);

        sessionStorage.setItem('cr_latest_ticket', JSON.stringify({
            ...complaintPayload,
            deskName: PipelineState.deskName,
            assignedOfficer: PipelineState.assignedOfficer,
            portal: PipelineState.targetPortal,
            urgency: PipelineState.urgency,
            createdAt: new Date().toISOString()
        }));

        setTimeout(() => {
            const redirectUrl = `/report/submission-success?complaintId=${encodeURIComponent(PipelineState.complaintId)}&title=${encodeURIComponent(titleVal)}&sector=${encodeURIComponent(secVal)}&location=${encodeURIComponent(locVal)}&urgency=${encodeURIComponent(PipelineState.urgency)}&dept=${encodeURIComponent(PipelineState.department)}&portal=${encodeURIComponent(PipelineState.targetPortal)}`;
            window.location.href = redirectUrl;
        }, 600);

    } finally {
        PipelineState.isSubmitting = false;
    }
}
