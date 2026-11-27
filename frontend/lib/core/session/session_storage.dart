import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract class TokenStorage {
  Future<void> saveToken(String token, [String? username]);
  Future<String?> readToken();
  Future<String?> readUsername();
  Future<void> saveUsername(String username);
  Future<void> clearToken();
}

class SecureTokenStorage implements TokenStorage {
  SecureTokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  static const _tokenKey = 'gestopago_session_token';
  static const _usernameKey = 'gestopago_session_username';
  final FlutterSecureStorage _storage;

  @override
  Future<void> saveToken(String token, [String? username]) async {
    await _storage.write(key: _tokenKey, value: token);
    if (username != null) {
      await _storage.write(key: _usernameKey, value: username);
    }
  }

  @override
  Future<void> saveUsername(String username) =>
      _storage.write(key: _usernameKey, value: username);

  @override
  Future<String?> readToken() => _storage.read(key: _tokenKey);

  @override
  Future<String?> readUsername() => _storage.read(key: _usernameKey);

  @override
  Future<void> clearToken() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _usernameKey);
  }
}

class InMemoryTokenStorage implements TokenStorage {
  String? _token;
  String? _username;

  @override
  Future<void> clearToken() async {
    _token = null;
    _username = null;
  }

  @override
  Future<String?> readToken() async => _token;

  @override
  Future<String?> readUsername() async => _username;

  @override
  Future<void> saveToken(String token, [String? username]) async {
    _token = token;
    if (username != null) {
      _username = username;
    }
  }

  @override
  Future<void> saveUsername(String username) async {
    _username = username;
  }
}

final tokenStorageProvider = Provider<TokenStorage>(
  (ref) => SecureTokenStorage(),
);
