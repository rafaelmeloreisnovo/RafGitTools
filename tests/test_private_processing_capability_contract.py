import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs/private-processing-capability.v1.json"
API = ROOT / "app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt"
REPOSITORY = ROOT / "app/src/main/kotlin/com/rafgittools/data/github/GithubRepository.kt"
RECEIPT = ROOT / "app/src/main/kotlin/com/rafgittools/bridge/PrivateProcessingReceiptV1.kt"


def test_private_processing_capability_is_fail_closed_and_private():
    cfg = json.loads(CONFIG.read_text(encoding="utf-8"))
    assert cfg["schema"] == "rafgittools.private-processing-capability/v1"
    assert cfg["claim_allowed"] is False
    assert cfg["authority"]["source_role"] == "READ_ONLY_SOURCE"
    assert cfg["authority"]["target_role"] == "PRIVATE_DERIVED_CUSTODY"

    lanes = {lane["id"]: lane for lane in cfg["lanes"]}
    assert lanes["ANDROID_EXPLICIT_USER"]["raw_payload_upload"] is False
    assert lanes["SECRET_AUTOMATION"]["state"] == "TOKEN_VAZIO_UNWIRED_SECRET"
    assert "PAT_ENV" in lanes["SECRET_AUTOMATION"]["fallback_secrets_forbidden"]
    assert "PAT_ACTIONS" in lanes["SECRET_AUTOMATION"]["fallback_secrets_forbidden"]


def test_public_contract_does_not_embed_private_drive_ids_or_target_repo_name():
    text = CONFIG.read_text(encoding="utf-8")
    assert "drive.google.com" not in text
    assert "CONVERSATIONS_CHUNKS_PRIVATE" not in text
    assert "1P7hJq5R4fgYGEQIVNgRvllAad2lGxWEv" not in text


def test_private_writer_requires_live_private_readback_and_namespace():
    api = API.read_text(encoding="utf-8")
    repository = REPOSITORY.read_text(encoding="utf-8")
    receipt = RECEIPT.read_text(encoding="utf-8")

    assert '@PUT("repos/{owner}/{repo}/contents/{path}")' in api
    assert 'path.startsWith("memory_bridge/private_processing/")' in repository
    assert "githubApiService.getRepository(owner, repo)" in repository
    assert "require(liveTarget.isPrivate)" in repository
    assert "rawPayloadUploaded: Boolean = false" in receipt
    assert "claimAllowed: Boolean = false" in receipt
    assert '"PRIVATE_ACTIVITY_RECEIPT_ONLY"' in receipt
