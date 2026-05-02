{ pkgs, ... }:

{
  # Which nixpkgs channel to use.
  channel = "stable-23.11"; # or "unstable"

  # Use https://search.nixos.org/packages to find packages
  packages = [
    pkgs.python311
    # pkgs.go
    # pkgs.sbt
    pkgs.gradle
    # pkgs.unzip
  ];

  # Sets environment variables in the workspace
  env = {
    # GRPC_HEALTH_PROBE_VERSION = "v0.4.24";
  };

  # Enter the shell environment automatically
  # devcontainer.shell = "/bin/zsh";
}
