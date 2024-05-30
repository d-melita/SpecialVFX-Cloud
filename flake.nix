{
  description = "CNV Project";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-23.05";
  };

  outputs = { self, nixpkgs }: 
  let pkgs = import nixpkgs { system = "x86_64-linux"; }; in {
      devShell.x86_64-linux = pkgs.mkShell {
        buildInputs = with pkgs; [
          jdk17 jre8 maven
          awscli2
          rsync
          python310 python311Packages.jupyter-core
        ];
      };
  };
}
