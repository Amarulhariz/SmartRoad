import { signInWithEmailAndPassword, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { doc, getDoc } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";
import { auth, db } from "./firebase-config.js";

const form = document.getElementById("loginForm");
const errorText = document.getElementById("errorText");

const NOT_ADMIN_MESSAGE = "This account doesn't have admin access";

const storedError = sessionStorage.getItem("loginError");
if (storedError) {
  errorText.textContent = storedError;
  sessionStorage.removeItem("loginError");
}

async function isAdminUser(user) {
  const snap = await getDoc(doc(db, "users", user.uid));
  return snap.exists() && snap.data().isAdmin === true;
}

// Guards against the auto-redirect listener below and the submit handler
// both reacting to the same sign-in and racing each other.
let handlingAuthChange = false;

async function admitIfAdmin(user) {
  if (await isAdminUser(user)) {
    window.location.href = "dashboard.html";
  } else {
    await signOut(auth);
    errorText.textContent = NOT_ADMIN_MESSAGE;
  }
}

onAuthStateChanged(auth, async (user) => {
  if (!user || handlingAuthChange) {
    return;
  }
  handlingAuthChange = true;
  try {
    await admitIfAdmin(user);
  } finally {
    handlingAuthChange = false;
  }
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  errorText.textContent = "";

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;
  const submitBtn = form.querySelector("button[type='submit']");

  submitBtn.disabled = true;
  handlingAuthChange = true;
  try {
    const credential = await signInWithEmailAndPassword(auth, email, password);
    await admitIfAdmin(credential.user);
  } catch (err) {
    errorText.textContent = "Login failed: " + err.message;
  } finally {
    submitBtn.disabled = false;
    handlingAuthChange = false;
  }
});
