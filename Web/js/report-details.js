import { signOut } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { doc, getDoc, updateDoc, deleteDoc } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { getStorage, ref, deleteObject } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-storage.js";
import { auth, db, app } from "./firebase-config.js";
import { requireAuth } from "./auth-guard.js";

const params = new URLSearchParams(window.location.search);
const reportId = params.get("id");
const detailCard = document.getElementById("detailCard");

const HAZARD_TYPES = [
  "Pothole", "Flood", "Accident", "Fallen Tree", "Traffic Light", "Damaged Road Sign"
];

function statusBadgeClass(status) {
  if (status === "Resolved") return "badge-resolved";
  if (status === "Under Investigation") return "badge-investigating";
  return "badge-new";
}

async function resolveUserLabel(userId) {
  if (!userId) return "Unknown";
  try {
    const userDoc = await getDoc(doc(db, "users", userId));
    if (!userDoc.exists()) return userId;
    const data = userDoc.data();
    return `${data.name || "Unknown"} (@${data.username || "?"})`;
  } catch (err) {
    console.warn("Failed to resolve user label:", err);
    return "Unknown";
  }
}

async function loadReport() {
  try {
    await loadReportUnsafe();
  } catch (err) {
    console.error("Failed to load report:", err);
    detailCard.innerHTML = `<p>Failed to load this report: ${err.message}</p>`;
  }
}

async function loadReportUnsafe() {
  if (!reportId) {
    detailCard.innerHTML = "<p>No report ID provided.</p>";
    return;
  }

  const reportRef = doc(db, "reports", reportId);
  const snap = await getDoc(reportRef);

  if (!snap.exists()) {
    detailCard.innerHTML = "<p>Report not found.</p>";
    return;
  }

  const report = snap.data();
  const status = report.status || "New";
  const userLabel = await resolveUserLabel(report.userId);
  const dateText = report.timestamp ? new Date(report.timestamp).toLocaleString() : "—";

  detailCard.innerHTML = `
    ${report.photoUrl ? `<img src="${report.photoUrl}" alt="Hazard photo" />` : ""}

    <div class="detail-row">
      <div class="key">Hazard Type</div>
      <div class="val">
        <select id="editHazardType">
          ${HAZARD_TYPES.map((type) =>
            `<option value="${type}" ${report.hazardType === type ? "selected" : ""}>${type}</option>`
          ).join("")}
        </select>
      </div>
    </div>
    <div class="detail-row">
      <div class="key">Status</div>
      <div class="val"><span class="badge ${statusBadgeClass(status)}">${status}</span></div>
    </div>
    <div class="detail-row">
      <div class="key">Description</div>
      <div class="val">
        <textarea id="editDescription" rows="3" style="width:100%; padding:8px; border:1px solid var(--border); border-radius:6px; font-size:14px;">${report.description || ""}</textarea>
      </div>
    </div>
    <div class="detail-row">
      <div class="key">Reported By</div>
      <div class="val">${userLabel}</div>
    </div>
    <div class="detail-row">
      <div class="key">Date/Time</div>
      <div class="val">${dateText}</div>
    </div>
    <div class="detail-row">
      <div class="key">Coordinates</div>
      <div class="val">${report.latitude ?? "—"}, ${report.longitude ?? "—"}</div>
    </div>
    <div class="detail-row">
      <div class="key">User Agent</div>
      <div class="val">${report.userAgent || "—"}</div>
    </div>

    <div class="actions-bar">
      <button class="btn-primary" id="saveEditsBtn" style="margin-top:0;">Save Changes</button>
    </div>

    <div class="actions-bar">
      <select id="statusSelect">
        <option value="New" ${status === "New" ? "selected" : ""}>New</option>
        <option value="Under Investigation" ${status === "Under Investigation" ? "selected" : ""}>Under Investigation</option>
        <option value="Resolved" ${status === "Resolved" ? "selected" : ""}>Resolved</option>
      </select>
      <button class="btn-primary" id="updateStatusBtn" style="margin-top:0;">Update Status</button>
      <button class="btn-danger" id="deleteBtn">Delete Report</button>
    </div>
    <div class="error-text" id="actionMessage"></div>
  `;

  document.getElementById("saveEditsBtn").addEventListener("click", async () => {
    const newType = document.getElementById("editHazardType").value;
    const newDescription = document.getElementById("editDescription").value.trim();
    const messageEl = document.getElementById("actionMessage");

    if (!newDescription) {
      messageEl.style.color = "#C43E00";
      messageEl.textContent = "Description cannot be empty.";
      return;
    }

    try {
      await updateDoc(reportRef, { hazardType: newType, description: newDescription });
      messageEl.style.color = "#2E7D32";
      messageEl.textContent = "Changes saved.";
      loadReport();
    } catch (err) {
      messageEl.style.color = "#C43E00";
      messageEl.textContent = "Failed to save changes: " + err.message;
    }
  });

  document.getElementById("updateStatusBtn").addEventListener("click", async () => {
    const newStatus = document.getElementById("statusSelect").value;
    const messageEl = document.getElementById("actionMessage");
    try {
      await updateDoc(reportRef, { status: newStatus });
      messageEl.style.color = "#2E7D32";
      messageEl.textContent = "Status updated.";
      loadReport();
    } catch (err) {
      messageEl.style.color = "#C43E00";
      messageEl.textContent = "Failed to update status: " + err.message;
    }
  });

  document.getElementById("deleteBtn").addEventListener("click", async () => {
    if (!confirm("Delete this report permanently? This cannot be undone.")) return;

    try {
      if (report.photoUrl) {
        const storage = getStorage(app);
        await deleteObject(ref(storage, report.photoUrl)).catch((photoErr) => {
          console.warn("Report deleted, but photo removal from Storage failed:", photoErr);
        });
      }
      await deleteDoc(reportRef);
      window.location.href = "reports.html";
    } catch (err) {
      document.getElementById("actionMessage").textContent = "Failed to delete: " + err.message;
    }
  });
}

requireAuth(loadReport);

document.getElementById("logoutLink").addEventListener("click", async (e) => {
  e.preventDefault();
  await signOut(auth);
  window.location.href = "login.html";
});
