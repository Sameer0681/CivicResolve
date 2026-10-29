(() => {
    const mapElement = document.getElementById("incidentMap");
    if (!mapElement) return;

    const status = document.getElementById("mapPickerStatus");
    const addressInput = document.getElementById("formLocationInput");
    const locateButton = document.getElementById("mapLocateButton");
    const latitudeInput = document.getElementById("incidentLatitude");
    const longitudeInput = document.getElementById("incidentLongitude");

    if (typeof L === "undefined") {
        status.textContent = "The map could not load. Enter the incident address manually to continue.";
        locateButton.disabled = true;
        return;
    }

    const defaultCenter = [20.5937, 78.9629];
    const map = L.map(mapElement).setView(defaultCenter, 5);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    let marker = null;
    function showStatus(message, isError = false) {
        status.textContent = message;
        status.classList.toggle("text-danger", isError);
        status.classList.toggle("text-muted", !isError);
    }

    function setCoordinates(latitude, longitude) {
        latitudeInput.value = latitude.toFixed(6);
        longitudeInput.value = longitude.toFixed(6);
    }

    async function reverseLookup(latitude, longitude) {
        const selectedPoint = `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`;
        const params = new URLSearchParams({
            format: "jsonv2",
            lat: latitude,
            lon: longitude
        });
        const response = await fetch(`https://nominatim.openstreetmap.org/reverse?${params}`, {
            headers: { Accept: "application/json" }
        });
        if (!response.ok) throw new Error(`Address lookup failed (${response.status}).`);
        const result = await response.json();
        if (result.display_name) addressInput.value = result.display_name.slice(0, 255);
        showStatus(`Map point selected: ${selectedPoint}.`);
    }

    function selectPoint(latitude, longitude, zoom = 16) {
        const point = [latitude, longitude];
        setCoordinates(latitude, longitude);
        if (!marker) {
            marker = L.marker(point, { draggable: true }).addTo(map);
            marker.on("dragend", () => {
                const moved = marker.getLatLng();
                selectPoint(moved.lat, moved.lng, map.getZoom());
            });
        } else {
            marker.setLatLng(point);
        }
        map.setView(point, zoom);
        reverseLookup(latitude, longitude).catch(error => {
            showStatus(`${error.message} Coordinates were saved; enter the address manually if needed.`, true);
        });
    }

    map.on("click", event => selectPoint(event.latlng.lat, event.latlng.lng));

    locateButton.addEventListener("click", () => {
        if (!navigator.geolocation) {
            showStatus("This browser does not support device location. Click the map or enter the address manually.", true);
            return;
        }
        locateButton.disabled = true;
        showStatus("Finding your current location…");
        navigator.geolocation.getCurrentPosition(
            position => {
                locateButton.disabled = false;
                selectPoint(position.coords.latitude, position.coords.longitude);
            },
            error => {
                locateButton.disabled = false;
                const message = error.code === error.PERMISSION_DENIED
                    ? "Location permission was denied. Click the map or enter the address manually."
                    : error.code === error.TIMEOUT
                        ? "Finding your location timed out. Try again or choose a point on the map."
                        : "Your location is unavailable. Click the map or enter the address manually.";
                showStatus(message, true);
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
        );
    });

    addressInput.addEventListener("input", () => {
        if (latitudeInput.value || longitudeInput.value) {
            latitudeInput.value = "";
            longitudeInput.value = "";
            showStatus("Address changed. Select a map point again if you want to attach coordinates.");
        }
    });

    const initialLatitude = Number(latitudeInput.value);
    const initialLongitude = Number(longitudeInput.value);
    if (latitudeInput.value && longitudeInput.value
        && Number.isFinite(initialLatitude) && Number.isFinite(initialLongitude)) {
        marker = L.marker([initialLatitude, initialLongitude], { draggable: true }).addTo(map);
        marker.on("dragend", () => {
            const moved = marker.getLatLng();
            selectPoint(moved.lat, moved.lng, map.getZoom());
        });
        map.setView([initialLatitude, initialLongitude], 16);
    }
})();
