# GoogleCloudKmsApplication Documentation

## Overview

`GoogleCloudKmsApplication` is a self-contained Java application that demonstrates how to use **Google Cloud KMS** (Key Management Service) to:

1. Create a **KeyRing** and an **RSA asymmetric signing key** if they don’t already exist.
2. Fetch the **public key** from KMS and save it to a file (`pubkey.pem`).
3. Sign a message using the KMS key and save the signature to a file (`sig.bin`).
4. Verify the signature locally using Java’s `SHA256withRSA` algorithm.

This class provides an end-to-end workflow for asymmetric signing with KMS in a **fully programmatic way**, without relying on any third-party or proprietary libraries.

---

## Prerequisites

* Java 11+
* Google Cloud KMS enabled in your project
* A service account with the following IAM permissions:

    * `cloudkms.cryptoKeyVersions.useToSign`
    * `cloudkms.cryptoKeyVersions.getPublicKey`
    * `cloudkms.keyRings.create`
    * `cloudkms.cryptoKeys.create`
* Google Cloud SDK configured (optional for local testing)
* Maven or Gradle with `com.google.cloud:google-cloud-kms` dependency



---

## 1️⃣ Google Cloud Setup for New Users

Follow these steps to set up Google Cloud KMS for the first time:

### Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Click **Select a project → New Project**.
3. Enter a **Project Name** and click **Create**.
4. Note the **Project ID** — you’ll need it in the Java code.

### Step 2: Enable Billing

1. In the console, navigate to **Billing → Manage Billing Accounts**.
2. Ensure your project has a **billing account attached**.
3. Without billing enabled, KMS creation will fail.

### Step 3: Enable APIs

Enable the required APIs for KMS:

```bash
gcloud services enable cloudkms.googleapis.com
```
Here’s the updated version of Steps 3 and 4 with GUI instructions and local authentication included:

---

### Step 3: Enable APIs

You need to enable the **Cloud KMS API** so your project can create and use keys.

**Option 1 – Using the Console (GUI):**

1. Go to [Google Cloud Console → APIs & Services → Library](https://console.cloud.google.com/apis/library).
2. Search for **Cloud Key Management Service (KMS) API**.
3. Click **Enable**.

**Option 2 – Using gcloud CLI:**

```bash
gcloud services enable cloudkms.googleapis.com
```

---

### Step 4: Create a Service Account and Set Up Local Authentication

You have **two options** for authentication:

**Option A — Easiest for local testing (using your Google account)**

1. Open a terminal or PowerShell.
2. Run the following command:

```bash
gcloud auth application-default login
```

3. A browser window will open. Log in with your Google account.
4. This will generate **application-default credentials** on your machine that your Java code can use automatically.

> ✅ Note: This is convenient for local development but not recommended for production environments.

---

**Option B — Using a Service Account JSON (recommended for production)**

1. In the **Google Cloud Console**, go to **IAM & Admin → Service Accounts**.
2. Click **Create Service Account**, give it a descriptive name (e.g., `kms-signer`).
3. Assign these roles:

    * `Cloud KMS Admin`
    * `Cloud KMS CryptoKey Signer/Verifier`
4. Click **Done**.
5. Click on the service account → **Keys → Add Key → Create new key → JSON**.
6. Save the JSON file locally (e.g., `key.json`).
7. Set the environment variable for local authentication:

---

### Step 5: Set Credentials Locally

**Windows (PowerShell):**

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\key.json"
```

**Linux / macOS:**

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/key.json"
```
---

## Configuration

Before running the program, update the following variables in the `main()` method:

```java
String projectId = "your-gcp-project-id";      // Google Cloud Project ID
String locationId = "global";                   // KMS location, e.g., "us-central1"
String keyRingId = "my-key-ring";              // KeyRing name
String keyId = "test-asymmetric-key";          // CryptoKey name
String message = "hello world";                 // Message to sign
```

---

## Functionality

### 1. KeyRing Management

* The application checks if the KeyRing exists.
* If it doesn’t, it automatically creates a new KeyRing in the specified location.

### 2. CryptoKey Management

* Checks if the asymmetric signing key exists.
* If not, creates a new **RSA_SIGN_PKCS1_2048_SHA256** CryptoKey for signing.
* This ensures that signing operations can be performed without manual setup.

### 3. Public Key Retrieval

* Fetches the **public key** for the first key version.
* Saves it to `pubkey.pem` in standard PEM format.
* Can be used externally for signature verification.

### 4. Signing a Message

* Computes the SHA-256 digest of the input message.
* Uses KMS to create an **asymmetric signature**.
* Saves the signature as a binary file (`sig.bin`).
* Prints the signature in Base64 format for reference.

### 5. Signature Verification

* Loads the public key from `pubkey.pem`.
* Uses Java `Signature` class with `SHA256withRSA` to verify the signature.
* Prints `true` if the signature matches the message, otherwise `false`.

---

## Output

When the program runs successfully, it produces:

1. `pubkey.pem` – PEM-formatted public key.
2. `sig.bin` – binary signature of the message.
3. Console output showing:

    * KeyRing and key status
    * Base64 signature
    * Verification result

Example console output:

```
KeyRing exists: my-key-ring
Key exists: test-asymmetric-key
Saved public key to pubkey.pem
Saved signature to sig.bin
Signature (base64): NHSIk6GlbrowC2OSGD...
Signature verified: true
```

---

## How to Run

1. Ensure the service account credentials are set:

**Windows (PowerShell)**

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\your\service-account.json"
```

**Linux / macOS**

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
```

2. Compile and run the program:

```bash
mvn compile exec:java -Dexec.mainClass="com.example.googlecloudkms.GoogleCloudKmsSigning"
```

3. Verify that `pubkey.pem` and `sig.bin` are created in the project directory.

---

## Notes

* The program **automatically handles key creation**, so it’s safe to run multiple times.
* The verification uses the **original message**, not the hash, because KMS uses PKCS#1 v1.5 padding with SHA-256.
* This class is intended for **demonstration and testing purposes**. For production, handle key lifecycle and file storage according to your security policies.

