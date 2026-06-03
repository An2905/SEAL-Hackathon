#!/usr/bin/env python3
import os
import re
import sys

def is_safe_sql_concatenation(expression):
    # Replace Java 17 text blocks (triple quotes)
    placeholder_expr = re.sub(r'"""[\s\S]*?"""', '__STR__', expression)
    # Replace standard double-quoted string literals
    placeholder_expr = re.sub(r'"([^"\\]|\\.)*"', '__STR__', placeholder_expr)
    
    # Split by +
    parts = [p.strip() for p in placeholder_expr.split('+')]
    
    for part in parts:
        # Strip outer parentheses
        while part.startswith('(') and part.endswith(')'):
            part = part[1:-1].strip()
        if not part:
            continue
            
        # Allowed operands in SQL string constructions
        if part == '__STR__' or part.isdigit() or part.lower() in ['true', 'false', 'null', '""', "''"]:
            continue
            
        # If there is any other token, it represents a variable or method call
        return False
    return True

def check_line_for_sql_injection(line, file_path, line_no):
    # Skip commented lines
    line_stripped = line.strip()
    if not line_stripped or line_stripped.startswith('//') or line_stripped.startswith('*') or line_stripped.startswith('/*'):
        return None

    # 1. Prohibit createStatement
    if "createStatement(" in line:
        return (
            f"[SQL INJECTION VIOLATION] at {file_path}:{line_no}\n"
            f"   Code: {line_stripped}\n"
            f"   Reason: Use of createStatement() is strictly prohibited. Always use prepareStatement() with '?' placeholders to enforce parameter-binding."
        )

    # 2. Check for string concatenation in SQL definitions
    sql_var_match = re.search(r'\b(sql|query|sqlQuery|selectQuery|insertQuery|updateQuery|deleteQuery)\b\s*(\+)?=', line, re.IGNORECASE)
    has_sql_literal = re.search(r'"\s*(SELECT|INSERT INTO|UPDATE|DELETE FROM|MERGE INTO)\b', line, re.IGNORECASE)

    if sql_var_match or has_sql_literal:
        if '+' in line:
            # Isolate expression
            if '=' in line:
                expr = line.split('=', 1)[1].strip()
            else:
                expr = line.strip()
            
            if expr.endswith(';'):
                expr = expr[:-1].strip()
                
            if not is_safe_sql_concatenation(expr):
                return (
                    f"[SQL INJECTION VIOLATION] at {file_path}:{line_no}\n"
                    f"   Code: {line_stripped}\n"
                    f"   Reason: Unsafe SQL string concatenation detected. Concatenating variables directly into SQL strings causes high vulnerability. Use '?' placeholders and PreparedStatement parameter-binding instead."
                )

        # 3. Check for dynamic string formatting on SQL
        if any(fmt in line for fmt in ["String.format(", ".formatted(", "printf("]):
            return (
                f"[SQL INJECTION VIOLATION] at {file_path}:{line_no}\n"
                f"   Code: {line_stripped}\n"
                f"   Reason: Use of String.format() or dynamic formatting with SQL statements is prohibited. Always use '?' placeholders and PreparedStatement parameter-binding."
            )

    return None

def check_properties_file(file_path, file_content):
    violations = []
    lines = file_content.splitlines()
    for i, line in enumerate(lines, 1):
        line = line.strip()
        if not line or line.startswith('#') or line.startswith('!'):
            continue
        if '=' in line:
            key, val = [part.strip() for part in line.split('=', 1)]
            sensitive_keys = ['password', 'secret', 'api-key', 'apikey', 'private-key', 'token', 'jwt']
            if any(s_key in key.lower() for s_key in sensitive_keys):
                # If there's a non-empty value and not a placeholder
                if val and not (val.startswith('${') and val.endswith('}')):
                    local_defaults = ['sa', 'root', 'localhost', '12345', '123456', 'password', 'true', 'false']
                    if val.lower() not in local_defaults:
                        violations.append(
                            f"[HARDCODED SECRET VIOLATION] at {file_path}:{i}\n"
                            f"   Line: {line}\n"
                            f"   Reason: Sensitive configuration key '{key}' contains a hardcoded secret. Extract this key to an environment variable reference (e.g., '${{VAR_NAME}}') to prevent committing production secrets to source control."
                        )
    return violations

def check_java_secrets(file_path, file_content):
    violations = []
    # Strip comments
    content_no_comments = re.sub(r'/\*.*?\*/', '', file_content, flags=re.DOTALL)
    content_no_comments = re.sub(r'//.*?\n', '\n', content_no_comments)
    
    lines = content_no_comments.splitlines()
    for i, line in enumerate(lines, 1):
        line_stripped = line.strip()
        sensitive_match = re.search(
            r'\b(String|char\[\]|var)\s+(jwtSecret|apiKey|api_key|secretKey|secret_key|dbPassword|db_password|smtpPassword|mailPassword)\b\s*=\s*(.*)', 
            line_stripped, re.IGNORECASE
        )
        if sensitive_match:
            rhs = sensitive_match.group(3).strip()
            if rhs.endswith(';'):
                rhs = rhs[:-1].strip()
                
            str_literal_match = re.match(r'^"([^"\\]|\\.)*"$', rhs)
            if str_literal_match:
                val = str_literal_match.group(0).strip('"')
                if val and not (val.startswith('${') and val.endswith('}')):
                    local_defaults = ['placeholder', 'secret', 'password', 'key', 'xxxx', '12345', '123456']
                    if val.lower() not in local_defaults and len(val) > 8:
                        violations.append(
                            f"[HARDCODED SECRET VIOLATION] at {file_path}:{i}\n"
                            f"   Code: {line_stripped}\n"
                            f"   Reason: Sensitive variable is assigned a hardcoded string literal. Please extract secrets to environment variables and load them via application.properties configurations."
                        )
    return violations

def scan_files(directory):
    sql_violations = []
    secret_violations = []
    
    for root, _, files in os.walk(directory):
        # Exclude ignored directories like .git, node_modules, target, etc.
        if any(ignored in root.replace('\\', '/').split('/') for ignored in ['.git', 'node_modules', 'target', '.agents', '.antigravitycli', '.vscode']):
            continue
            
        for file in files:
            file_path = os.path.join(root, file)
            
            # Read file contents
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
            except Exception as e:
                print(f"Skipping file {file_path} due to read error: {e}")
                continue
                
            # Scan based on extension
            if file.endswith('.java'):
                # SQL Injection scan
                # Remove comments for clean lines parsing
                content_no_comments = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
                lines = content_no_comments.splitlines()
                for i, line in enumerate(lines, 1):
                    violation = check_line_for_sql_injection(line, file_path, i)
                    if violation:
                        sql_violations.append(violation)
                        
                # Hardcoded secrets scan
                secret_violations.extend(check_java_secrets(file_path, content))
                
            elif file.endswith('.properties') or file.endswith('.yml') or file.endswith('.yaml'):
                # Properties hardcoded secrets scan
                secret_violations.extend(check_properties_file(file_path, content))
                
    return sql_violations, secret_violations

def main():
    print("[SCANNING START] Starting Security Guards checks...")
    
    # Scan backend/src/
    search_dir = os.path.join(os.getcwd(), 'backend', 'src')
    if not os.path.exists(search_dir):
        # Fall back to root directory scan if backend/src doesn't exist
        search_dir = os.getcwd()
        
    print(f"Target Directory: {search_dir}")
    
    sql_violations, secret_violations = scan_files(search_dir)
    
    # Display results
    total_violations = len(sql_violations) + len(secret_violations)
    
    print("\n" + "="*50)
    print("SECURITY SCAN REPORT (expert-reviewer guards)")
    print("="*50)
    
    if sql_violations:
        print(f"\nSQL INJECTION GUARD: FAILED ({len(sql_violations)} violations)")
        for v in sql_violations:
            print("-"*50)
            print(v)
    else:
        print("\nSQL INJECTION GUARD: PASSED (no concatenations found)")
        
    if secret_violations:
        print(f"\nHARDCODED SECRETS GUARD: FAILED ({len(secret_violations)} violations)")
        for v in secret_violations:
            print("-"*50)
            print(v)
    else:
        print("\nHARDCODED SECRETS GUARD: PASSED (no committed credentials found)")
        
    print("\n" + "="*50)
    print(f"Summary: {total_violations} violations found.")
    print("="*50)
    
    if total_violations > 0:
        print("\nPull request verification failed! Please fix the security violations listed above before merging.")
        sys.exit(1)
    else:
        print("\nAll security scans passed! Your branch is clean and ready.")
        sys.exit(0)

if __name__ == "__main__":
    main()
