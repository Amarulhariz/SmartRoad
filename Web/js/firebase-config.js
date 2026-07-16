import { initializeApp } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyA45PIwNPoXrR5pS0xYc6_-rHCwoBJlepA",
  authDomain: "smartroad-9e81e.firebaseapp.com",
  projectId: "smartroad-9e81e",
  storageBucket: "smartroad-9e81e.firebasestorage.app",
  messagingSenderId: "60973690552",
  appId: "1:60973690552:web:5d1a378f0492d03f3fce4d",
  measurementId: "G-P73SGPZ22K"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
