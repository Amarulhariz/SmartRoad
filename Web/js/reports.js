import { signOut } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { collection, getDocs, doc, getDoc, addDoc, orderBy, query } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { getStorage, ref, uploadBytes, getDownloadURL } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-storage.js";
import { auth, db, app } from "./firebase-config.js";
import { requireAuth } from "./auth-guard.js";

const tableBody = document.getElementById("reportsTableBody");
const emptyState = document.getElementById("emptyState");
const searchInput = document.getElementById("searchInput");
const filterType = document.getElementById("filterType");
const filterStatus = document.getElementById("filterStatus");
const filterDate = document.getElementById("filterDate");

let allReports = [];
const userNameCache = new Map();

function statusBadgeClass(status) {
  if (status === "Resolved") return "badge-resolved";
  if (status === "Under Investigation") return "badge-investigating";
  return "badge-new";
}

async function resolveUserName(userId) {
  if (!userId) return "Unknown";
  if (userNameCache.has(userId)) return userNameCache.get(userId);

  const userDoc = await getDoc(doc(db, "users", userId));
  const name = userDoc.exists() ? (userDoc.data().name || userDoc.data().username || userId) : userId;
  userNameCache.set(userId, name);
  return name;
}

function matchesFilters(report) {
  const search = searchInput.value.trim().toLowerCase();
  if (search) {
    const haystack = `${report.hazardType || ""} ${report.description || ""}`.toLowerCase();
    if (!haystack.includes(search)) return false;
  }
  if (filterType.value && report.hazardType !== filterType.value) return false;
  if (filterStatus.value && (report.status || "New") !== filterStatus.value) return false;

  if (filterDate.value && report.timestamp) {
    const reportDate = new Date(report.timestamp);
    const [year, month, day] = filterDate.value.split("-").map(Number);
    if (reportDate.getFullYear() !== year || reportDate.getMonth() + 1 !== month || reportDate.getDate() !== day) {
      return false;
    }
  }
  return true;
}

async function renderTable() {
  const filtered = allReports.filter(matchesFilters);
  tableBody.innerHTML = "";
  emptyState.style.display = filtered.length === 0 ? "block" : "none";

  for (const report of filtered) {
    const userName = await resolveUserName(report.userId);
    const row = document.createElement("tr");
    row.className = "clickable";
    row.addEventListener("click", () => {
      window.location.href = `report-details.html?id=${report.id}`;
    });

    const dateText = report.timestamp
      ? new Date(report.timestamp).toLocaleString()
      : "—";
    const status = report.status || "New";

    const thumbnail = report.photoUrl
      ? `<img class="thumbnail" src="${report.photoUrl}" alt="Hazard photo" />`
      : `<div class="thumbnail-placeholder"></div>`;

    row.innerHTML = `
      <td>${thumbnail}</td>
      <td>${report.hazardType || "—"}</td>
      <td>${(report.description || "").slice(0, 60)}</td>
      <td><span class="badge ${statusBadgeClass(status)}">${status}</span></td>
      <td>${dateText}</td>
      <td>${userName}</td>
    `;
    tableBody.appendChild(row);
  }
}

requireAuth(async () => {
  const snapshot = await getDocs(query(collection(db, "reports"), orderBy("timestamp", "desc")));
  allReports = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
  renderTable();
});

[searchInput, filterType, filterStatus, filterDate].forEach((el) =>
  el.addEventListener("input", renderTable)
);

document.getElementById("clearFilters").addEventListener("click", () => {
  searchInput.value = "";
  filterType.value = "";
  filterStatus.value = "";
  filterDate.value = "";
  renderTable();
});

document.getElementById("logoutLink").addEventListener("click", async (e) => {
  e.preventDefault();
  await signOut(auth);
  window.location.href = "login.html";
});

const addReportOverlay = document.getElementById("addReportOverlay");
const addReportForm = document.getElementById("addReportForm");
const addReportError = document.getElementById("addReportError");

document.getElementById("openAddReportBtn").addEventListener("click", () => {
  addReportForm.reset();
  addReportError.textContent = "";
  addReportOverlay.style.display = "flex";
});

document.getElementById("cancelAddReportBtn").addEventListener("click", () => {
  addReportOverlay.style.display = "none";
});

function fileExtension(file) {
  const parts = file.name.split(".");
  return parts.length > 1 ? parts.pop() : "jpg";
}

async function uploadReportPhoto(file) {
  const storage = getStorage(app);
  const timestamp = Date.now();
  const photoRef = ref(storage,
      `hazard_photos/${auth.currentUser.uid}_${timestamp}.${fileExtension(file)}`);
  await uploadBytes(photoRef, file);
  return getDownloadURL(photoRef);
}

addReportForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  addReportError.textContent = "";

  const hazardType = document.getElementById("newHazardType").value;
  const description = document.getElementById("newDescription").value.trim();
  const status = document.getElementById("newStatus").value;
  const latitude = parseFloat(document.getElementById("newLatitude").value);
  const longitude = parseFloat(document.getElementById("newLongitude").value);
  const photoFile = document.getElementById("newPhoto").files[0];

  if (!description) {
    addReportError.textContent = "Description is required.";
    return;
  }
  if (Number.isNaN(latitude) || latitude < -90 || latitude > 90) {
    addReportError.textContent = "Enter a valid latitude between -90 and 90.";
    return;
  }
  if (Number.isNaN(longitude) || longitude < -180 || longitude > 180) {
    addReportError.textContent = "Enter a valid longitude between -180 and 180.";
    return;
  }

  const submitBtn = addReportForm.querySelector("button[type='submit']");
  submitBtn.disabled = true;
  submitBtn.textContent = photoFile ? "Uploading photo..." : "Saving...";

  try {
    const photoUrl = photoFile ? await uploadReportPhoto(photoFile) : null;

    const newReport = {
      userId: auth.currentUser.uid,
      hazardType,
      description,
      status,
      latitude,
      longitude,
      timestamp: Date.now(),
      photoUrl,
      userAgent: "Added via Admin Portal"
    };
    const docRef = await addDoc(collection(db, "reports"), newReport);
    allReports.unshift({ id: docRef.id, ...newReport });
    renderTable();
    addReportOverlay.style.display = "none";
  } catch (err) {
    addReportError.textContent = "Failed to save report: " + err.message;
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Save Report";
  }
});
