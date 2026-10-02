"""Check PR routes and the test-before-implementation commit sequence."""
import argparse
import re
import subprocess


def validate_route(source, target):
    allowed = (
        target == 'develop'
        and (source == 'master' or re.fullmatch(r'(feature|release|hotfix)/[^\s]+', source))
    ) or (
        target == 'master' and re.fullmatch(r'(release|hotfix)/[^\s]+', source)
    )
    return [] if allowed else [f'Rota GitFlow inválida: {source} -> {target}']


def validate_history(commits):
    errors = []
    pending_test = False
    for sha, subject, files in commits:
        match = re.match(r'^(\w+)(?:\([^)]*\))?!?:\s+\S', subject)
        kind = match.group(1) if match else None
        test_files = any(
            (p.startswith('src/test/') and p.endswith('Test.java'))
            or (p.startswith('scripts/tests/') and p.endswith('.py'))
            for p in files
        )
        production_files = any(p.startswith('src/main/') for p in files)
        if kind == 'test':
            if production_files:
                errors.append(f'{sha}: commit test altera código de produção; separar o RED do GREEN')
            elif test_files:
                pending_test = True
        elif kind in ('feat', 'fix'):
            if not pending_test:
                errors.append(f'{sha}: {kind} precisa de um commit test anterior com alteração de testes')
            pending_test = False
    return errors


def git(*args):
    return subprocess.check_output(['git', *args], text=True).strip()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True)
    parser.add_argument('--target', required=True)
    parser.add_argument('--base', required=True)
    parser.add_argument('--head', required=True)
    args = parser.parse_args()
    commits = []
    for sha in git('rev-list', '--reverse', '--topo-order', '--no-merges', f'{args.base}..{args.head}').splitlines():
        commits.append((
            sha[:12],
            git('show', '-s', '--format=%s', sha),
            git('show', '--format=', '--name-only', '--diff-filter=AM', sha).splitlines(),
        ))
    errors = validate_route(args.source, args.target) + validate_history(commits)
    if errors:
        for error in errors:
            print(error)
        return 1
    print('GitFlow e sequência test -> feat/fix aprovados.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
