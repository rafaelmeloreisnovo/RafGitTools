"""Negative gates for the provider-candidate path. Uses only fake provider responses."""
import importlib.util
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("fleet_gate_candidate",ROOT/"scripts/provider_fleet_gate_v1.py")
G=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(G)
SHA="b"*40
ENTRY={"name":"owner/demo","id":44,"default_branch":"main","head_sha":SHA,"visibility":"public"}

class Provider:
    def __init__(self,visibility="public",http_rules=200,protected=False,
                 badsha=False,reviewer_perm="write",deny_put=False,post_mismatch=False):
        self.visibility=visibility
        self.http_rules=http_rules
        self.protected=protected
        self.badsha=badsha
        self.reviewer_perm=reviewer_perm
        self.deny_put=deny_put
        self.post_mismatch=post_mismatch
        self.puts=0
    def request(self,method,path,payload=None):
        if path=="/repos/owner/demo":
            return 200,{"id":44,"full_name":"owner/demo","default_branch":"main",
                        "visibility":self.visibility,"permissions":{"admin":True}}
        if path=="/repos/owner/demo/branches/main":
            return 200,{"commit":{"sha":"c"*40 if self.badsha else SHA},
                        "protected":self.protected}
        if path=="/repos/owner/demo/rulesets":
            return self.http_rules,[] if self.http_rules==200 else None
        if path=="/repos/owner/demo/collaborators/reviewer/permission":
            return 200,{"permission":self.reviewer_perm}
        if path=="/repos/owner/demo/branches/main/protection":
            if method=="PUT":
                self.puts+=1
                if self.deny_put:
                    return 403,None
                self.protected=True
            if not self.protected:
                return 404,None
            return 200,{"enforce_admins":{"enabled":True},"allow_force_pushes":{"enabled":False},
                        "allow_deletions":{"enabled":False},
                        "required_conversation_resolution":{"enabled":not self.post_mismatch},
                        "required_pull_request_reviews":{
                            "required_approving_review_count":1,"dismiss_stale_reviews":True,
                            "require_last_push_approval":True}}
        return 403,None

class SecurityNegatives(unittest.TestCase):
    def test_missing_pat_fail_closed(self):
        for value in ("","x\ny","x\ry"):
            with self.subTest(value=repr(value)), self.assertRaises(G.Hold):
                G.APIClient(value)
    def test_private_provider_plan_denies_mutation(self):
        private=dict(ENTRY,visibility="private")
        p=Provider(visibility="private")
        r=G.execute(p,private,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_PRIVATE_POLICY_PLAN_OR_REVIEW_REQUIRED")
        self.assertEqual(p.puts,0)
    def test_403_ruleset_read_denies_mutation(self):
        p=Provider(http_rules=403)
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_PROVIDER_RULESETS_UNREADABLE_OR_PLAN")
        self.assertEqual(p.puts,0)
    def test_stale_sha_denies_mutation(self):
        p=Provider(badsha=True)
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_PROVIDER_BRANCH_SHA_DRIFT")
        self.assertEqual(p.puts,0)
    def test_permission_probe_denies_mutation(self):
        p=Provider(reviewer_perm="read")
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_INDEPENDENT_REVIEWER_NOT_VERIFIED")
        self.assertEqual(p.puts,0)
    def test_existing_protection_denies_overwrite(self):
        p=Provider(protected=True)
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_PREEXISTING_PROTECTION_HOLD")
        self.assertEqual(p.puts,0)
    def test_ambiguous_put_remains_hold(self):
        p=Provider(deny_put=True)
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_APPLY_NOT_CONFIRMED_POSSIBLE_MUTATION")
        self.assertEqual(p.puts,1)
        self.assertFalse(r["provider_write_verified"])
    def test_post_readback_fails_and_does_not_retry(self):
        p=Provider(post_mismatch=True)
        r=G.execute(p,ENTRY,SHA,"reviewer",apply=True)
        self.assertEqual(r["status"],"HOLD_POST_SECURITY_FLAGS_MISMATCH")
        self.assertFalse(r["provider_write_verified"])
        self.assertEqual(p.puts,1)
    def test_token_wont_escape_into_receipt(self):
        p=Provider()
        receipt=G.execute(p,ENTRY,SHA,"reviewer",apply=False)
        self.assertFalse(receipt["secret_value_recorded"])
        self.assertEqual(receipt["negative_rejection_test"],"NOT_RUN")

if __name__=="__main__":
    unittest.main()
