import pathlib
import struct
import tempfile
import unittest
import zipfile
import importlib.util

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('rafb_merge_unsigned', ROOT / 'rafb_merge_unsigned.py')
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def elf(bits, machine):
    h = bytearray(64)
    h[0:4] = b'\x7fELF'
    h[4:7] = bytes((bits, 1, 1))
    struct.pack_into('<HHI', h, 16, 3, machine, 1)
    struct.pack_into('<H', h, 40 if bits == 1 else 52, 52 if bits == 1 else 64)
    return bytes(h)


class MergeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.a = self.root / 'a.so'
        self.b = self.root / 'b.so'
        self.a.write_bytes(elf(1, 40))
        self.b.write_bytes(elf(2, 183))
        self.input = self.root / 'bootstrap-unsigned.apk'
        self.output = self.root / 'dual-unsigned.apk'
        self.write_boot()

    def write_boot(self, extra=None):
        with zipfile.ZipFile(self.input, 'w') as f:
            f.writestr('AndroidManifest.xml', b'AXML synthetic, not installable')
            f.writestr(m.BOOT[0], elf(1, 40))
            f.writestr(m.BOOT[1], elf(2, 183))
            for key, value in (extra or {}).items():
                f.writestr(key, value)

    def test_overlay_and_readback(self):
        m.run(self.input, self.a, self.b, self.output)
        self.assertTrue((self.root / 'dual-unsigned.apk.receipt.env').is_file())
        with zipfile.ZipFile(self.output) as f:
            self.assertEqual(f.read(m.NAMES[0]), self.a.read_bytes())
            self.assertEqual(f.read(m.NAMES[1]), self.b.read_bytes())
        self.assertTrue(self.input.exists())

    def test_no_overwrite(self):
        self.output.touch()
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)

    def test_reject_wrong_arm(self):
        self.a.write_bytes(elf(2, 183))
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)
        self.assertFalse(self.output.exists())

    def test_reject_missing_abi(self):
        with zipfile.ZipFile(self.input, 'w') as f:
            f.writestr('AndroidManifest.xml', b'AXML')
            f.writestr(m.BOOT[0], elf(1, 40))
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)

    def test_reject_signed_input(self):
        self.write_boot({'META-INF/CERT.RSA': b'fake'})
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)

    def test_reject_unsafe_path(self):
        self.write_boot({'../bad': b'x'})
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)

    def test_reject_duplicates(self):
        self.write_boot({m.NAMES[0]: elf(1, 40)})
        with self.assertRaises(ValueError):
            m.run(self.input, self.a, self.b, self.output)


if __name__ == '__main__':
    unittest.main()
