import { onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { auth, db } from "./firebase-config.js";

const NOT_ADMIN_MESSAGE = "This account doesn't have admin access";
const TIMEOUT_MS = 5000;

function showFatalError(message) {
  console.error("requireAuth failed:", message);
  document.body.style.visibility = "visible";
  document.body.innerHTML =
    `<div style="padding:32px; font-family:sans-serif;">
      <h2>Something went wrong</h2>
      <p>${message}</p>
      <p><a href="login.html">Return to login</a></p>
    </div>`;
}

export function requireAuth(onReady) {
  let settled = false;

  const timeoutId = setTimeout(() => {
    if (!settled) {
      settled = true;
      showFatalError("Loading timed out, please refresh.");
    }
  }, TIMEOUT_MS);

  console.log("[auth-guard] waiting for auth state...");

  onAuthStateChanged(auth, async (user) => {
    if (settled) {
      console.log("[auth-guard] already settled (timed out earlier), ignoring late auth state change");
      return;
    }

    console.log("[auth-guard] auth state received. user:", user ? user.uid : null);

    try {
      if (!user) {
        console.log("[auth-guard] no user, redirecting to login.html");
        settled = true;
        clearTimeout(timeoutId);
        window.location.href = "login.html";
        return;
      }

      console.log("[auth-guard] user found, checking isAdmin...");
      const snap = await getDoc(doc(db, "users", user.uid));
      const isAdmin = snap.exists() && snap.data().isAdmin === true;
      console.log("[auth-guard] isAdmin check complete:", isAdmin, "docExists:", snap.exists());

      if (!isAdmin) {
        console.log("[auth-guard] not an admin, signing out and redirecting");
        settled = true;
        clearTimeout(timeoutId);
        await signOut(auth);
        sessionStorage.setItem("loginError", NOT_ADMIN_MESSAGE);
        window.location.href = "login.html";
        return;
      }

      console.log("[auth-guard] admin confirmed, revealing page and calling onReady()...");
      settled = true;
      clearTimeout(timeoutId);
      document.body.style.visibility = "visible";
      await onReady(user);
      console.log("[auth-guard] onReady() completed");
    } catch (err) {
      settled = true;
      clearTimeout(timeoutId);
      showFatalError(err && err.message ? err.message : String(err));
    }
  });
}
