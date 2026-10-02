import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from gitflow_policy import validate_history, validate_route


class GitFlowPolicyTest(unittest.TestCase):
    def test_feature_targets_develop(self):
        self.assertEqual(validate_route('feature/12-pedido', 'develop'), [])
        self.assertTrue(validate_route('feature/12-pedido', 'master'))

    def test_release_and_hotfix_targets(self):
        for branch in ('release/1.0.0', 'hotfix/13-pedido'):
            for target in ('develop', 'master'):
                self.assertEqual(validate_route(branch, target), [])
        self.assertEqual(validate_route('master', 'develop'), [])
        self.assertTrue(validate_route('develop', 'master'))
        self.assertTrue(validate_route('deploy', 'develop'))
        self.assertTrue(validate_route('feature/', 'develop'))

    def test_test_commit_precedes_implementation(self):
        commits = [
            ('a', 'test: reproduzir erro', ['src/test/java/PedidoTest.java']),
            ('b', 'fix: corrigir pedido', ['src/main/java/Pedido.java']),
        ]
        self.assertEqual(validate_history(commits), [])

    def test_feature_without_prior_test_fails(self):
        self.assertTrue(validate_history([
            ('a', 'feat: criar pedido', ['src/main/java/Pedido.java']),
            ('b', 'test: testar pedido', ['src/test/java/PedidoTest.java']),
        ]))

    def test_same_commit_or_fake_test_does_not_satisfy_tdd(self):
        self.assertTrue(validate_history([
            ('a', 'test: preparar', ['README.md']),
            ('b', 'feat: pedido', ['src/main/java/Pedido.java', 'src/test/java/PedidoTest.java']),
        ]))
        self.assertTrue(validate_history([
            ('a', 'test: preparar', ['src/main/java/Pedido.java', 'src/test/java/PedidoTest.java']),
            ('b', 'feat: pedido', ['src/main/java/Pedido.java']),
        ]))

    def test_each_implementation_cycle_needs_a_test_commit(self):
        self.assertTrue(validate_history([
            ('a', 'test: preparar', ['src/test/java/PedidoTest.java']),
            ('b', 'feat: pedido', ['src/main/java/Pedido.java']),
            ('c', 'feat: outro pedido', ['src/main/java/Outro.java']),
        ]))

    def test_infrastructure_does_not_need_a_business_test(self):
        self.assertEqual(validate_history([
            ('a', 'ci: organizar pipeline', ['.github/workflows/verify.yaml']),
            ('b', 'docs: documentar GitFlow', ['docs/gitflow.md']),
        ]), [])


if __name__ == '__main__':
    unittest.main()
