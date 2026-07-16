import { signOut } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { collection, query, where, getCountFromServer } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { auth, db } from "./firebase-config.js";
import { requireAuth } from "./auth-guard.js";

requireAuth(async () => {
  const usersSnap = await getCountFromServer(collection(db, "users"));
  document.getElementById("statUsers").textContent = usersSnap.data().count;

  const reportsRef = collection(db, "reports");

  const totalSnap = await getCountFromServer(reportsRef);
  document.getElementById("statReports").textContent = totalSnap.data().count;

  const resolvedSnap = await getCountFromServer(query(reportsRef, where("status", "==", "Resolved")));
  const resolvedCount = resolvedSnap.data().count;
  document.getElementById("statResolved").textContent = resolvedCount;

  document.getElementById("statOpen").textContent = totalSnap.data().count - resolvedCount;
});

document.getElementById("logoutLink").addEventListener("click", async (e) => {
  e.preventDefault();
  await signOut(auth);
  window.location.href = "login.html";
});
