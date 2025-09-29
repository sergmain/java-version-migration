#!/usr/bin/env python3
"""
Copyright (c) 2023. Sergio Lissner

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Python port of Java migration classes
"""

import os
import sys
import time
import logging
import yaml
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Optional, Callable, Any, Union
from threading import Lock
import threading


# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


@dataclass
class Content:
    """Data class representing content and whether it was changed."""
    content: str
    changed: bool


@dataclass 
class Globals:
    """Global configuration for migration processing."""
    start_java_version: int = 0
    target_java_version: int = 21
    threads: int = 8
    charset: str = "utf-8"
    offset: int = 4  # number of spaces as offset in code
    starting_path: List[Path] = field(default_factory=lambda: [Path("src")])
    exclude_path: List[Path] = field(default_factory=list)
    file_mask: str = ".java"
    metas: List[Dict[str, str]] = field(default_factory=list)

    def __post_init__(self):
        """Initialize default values after object creation."""
        if not self.starting_path:
            self.starting_path = [Path("src")]

    @classmethod
    def from_yaml(cls, config_path: Union[str, Path] = "config/application.yml") -> 'Globals':
        """Create Globals instance from YAML configuration file."""
        config_file = Path(config_path)
        
        if not config_file.exists():
            logger.warning(f"Config file {config_file} not found, using defaults")
            return cls()
        
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                config_data = yaml.safe_load(f)
            
            migration_config = config_data.get('migration', {})
            
            # Convert paths to Path objects
            starting_paths = []
            if 'startingPath' in migration_config:
                for path_str in migration_config['startingPath']:
                    starting_paths.append(Path(path_str))
            
            exclude_paths = []
            if 'excludePath' in migration_config:
                for path_str in migration_config['excludePath']:
                    exclude_paths.append(Path(path_str))
            
            # Process metas - convert list of dicts with single key-value to list of dicts
            metas = []
            if 'metas' in migration_config:
                for meta_item in migration_config['metas']:
                    if isinstance(meta_item, dict):
                        metas.append(meta_item)
                    elif isinstance(meta_item, str) and ':' in meta_item:
                        # Handle "key: value" string format
                        key, value = meta_item.split(':', 1)
                        metas.append({key.strip(): value.strip()})
            
            return cls(
                start_java_version=migration_config.get('startJavaVersion', 0),
                target_java_version=migration_config.get('targetJavaVersion', 21),
                threads=migration_config.get('threads', 8),
                charset=migration_config.get('charset', 'utf-8'),
                offset=migration_config.get('offset', 4),
                starting_path=starting_paths if starting_paths else [Path("src")],
                exclude_path=exclude_paths,
                file_mask=migration_config.get('fileMask', '.java'),
                metas=metas
            )
            
        except Exception as e:
            logger.error(f"Error loading config from {config_file}: {e}")
            logger.info("Using default configuration")
            return cls()

    def to_yaml(self, output_path: Union[str, Path] = "config/application.yml"):
        """Save current configuration to YAML file."""
        config_data = {
            'migration': {
                'startJavaVersion': self.start_java_version,
                'targetJavaVersion': self.target_java_version,
                'threads': self.threads,
                'charset': self.charset,
                'offset': self.offset,
                'fileMask': self.file_mask,
                'startingPath': [str(path) for path in self.starting_path],
                'excludePath': [str(path) for path in self.exclude_path],
                'metas': self.metas
            }
        }
        
        output_file = Path(output_path)
        output_file.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            yaml.dump(config_data, f, default_flow_style=False, allow_unicode=True)


class Config:
    """Configuration class equivalent to Spring Configuration."""
    
    def __init__(self, globals_config: Globals):
        self.globals = globals_config
        logger.info("Configuration initialized")


@dataclass
class MigrationConfig:
    """Configuration for migration processing."""
    path: Path
    files: Dict[str, str]


@dataclass 
class MigrationFunctions:
    """Container for migration functions by version."""
    version: int
    functions: List[Callable[[MigrationConfig, Globals, str], Content]]


class MigrationUtils:
    """Utility functions for migration processing."""
    
    SECONDS_MILLIS = 1000
    
    @staticmethod
    def is_in_variable(content: str, start: int) -> bool:
        """Check if position is inside a variable/string."""
        line_start = MigrationUtils.search_start_line(content, start)
        try:
            quote_idx = content.index('"', line_start)
            return quote_idx != -1 and quote_idx < start
        except ValueError:
            return False
    
    @staticmethod
    def is_in_comment(content: str, start: int) -> bool:
        """Check if position is inside a comment."""
        return (MigrationUtils.is_in_comment_block(content, start) or 
                MigrationUtils.is_in_comment_line(content, start))
    
    @staticmethod
    def is_in_comment_block(content: str, start: int) -> bool:
        """Check if position is inside a block comment."""
        start_comment_left = content.rfind("/*", 0, start)
        end_comment_left = content.rfind("*/", 0, start)
        
        return ((start_comment_left != -1 and end_comment_left == -1) or 
                (start_comment_left > end_comment_left))
    
    @staticmethod
    def is_in_comment_line(content: str, start: int) -> bool:
        """Check if position is inside a line comment."""
        for i in range(start, -1, -1):
            if i >= len(content):
                continue
            c = content[i]
            if c in ['\n', '\r']:
                return False
            if c == '/' and i > 0 and content[i-1] == '/':
                return True
        return False
    
    @staticmethod
    def wait_task_completed(executor: ThreadPoolExecutor, number_of_periods: int = 100):
        """Wait for all tasks in executor to complete."""
        period_count = 0
        while executor._threads:  # Check if there are active threads
            time.sleep(1)  # Sleep for 1 second
            period_count += 1
            if period_count % number_of_periods == 0:
                print(f"Waiting for tasks to complete...")
                period_count = 0
    
    @staticmethod
    def exec_stat(start_mills: float, task_count: int) -> float:
        """Print execution statistics."""
        curr = time.time() * 1000
        sec = int((curr - start_mills) / 1000)
        message = f"\nProcessed {task_count} tasks for {sec} seconds"
        if sec != 0:
            message += f", {task_count // sec} tasks/sec"
        logger.info(message)
        return curr
    
    @staticmethod
    def search_start_line(content: str, start: int) -> int:
        """Search for the start of the line containing the given position."""
        for i in range(start - 1, -1, -1):
            if i < len(content):
                ch = content[i]
                if ch in ['\n', '\r']:
                    return i
        return 0


class Migration:
    """Migration processing logic."""
    
    @staticmethod
    def angular_to_signal_migration(config: MigrationConfig, globals_config: Globals, content: str) -> Content:
        """Angular to Signal migration implementation."""
        from angular_to_signal_migration import AngularToSignalMigration
        # The AngularToSignalMigration.process expects (config, content) not (config, globals, content)
        return AngularToSignalMigration.process(config, content)
    
    # Define migration functions by version
    functions: List[MigrationFunctions] = [
        MigrationFunctions(21, [angular_to_signal_migration])
    ]
    
    # Sort by version
    functions.sort(key=lambda x: x.version)


class MigrationProcessor:
    """Main migration processor."""
    
    total_size = 0
    total_size_lock = Lock()
    
    @staticmethod
    def migration_processor(globals_config: Globals):
        """Main migration processing function."""
        start_time = time.time() * 1000
        task_count = 0
        files_found = []
        
        # First, collect all files to process
        for starting_path in globals_config.starting_path:
            if not starting_path.exists():
                logger.warning(f"Starting path does not exist: {starting_path}")
                continue
                
            if not starting_path.is_dir():
                logger.warning(f"Starting path is not a directory: {starting_path}")
                continue
            
            logger.info(f"Scanning directory: {starting_path}")
            # Walk through directory and find matching files
            for file_path in starting_path.rglob(f"*{globals_config.file_mask}"):
                if MigrationProcessor._filter_path(file_path, globals_config.exclude_path):
                    files_found.append(file_path)
        
        logger.info(f"Found {len(files_found)} files to process")
        
        with ThreadPoolExecutor(max_workers=globals_config.threads) as executor:
            futures = []
            
            for file_path in files_found:
                future = executor.submit(MigrationProcessor._process, file_path, globals_config)
                futures.append(future)
                task_count += 1
            
            # Wait for all tasks to complete
            for future in as_completed(futures):
                try:
                    future.result()
                except Exception as e:
                    logger.error(f"Error processing file: {e}")
        
        MigrationUtils.exec_stat(start_time, task_count)
        print(f"Total size of files: {MigrationProcessor.total_size}")
    
    @staticmethod
    def _filter_path(path: Path, exclude_paths: List[Path]) -> bool:
        """Filter paths based on exclude list."""
        path_str = str(path)
        for exclude_path in exclude_paths:
            exclude_str = str(exclude_path)
            if exclude_str in path_str or path.name == exclude_path.name:
                return False
        return True
    
    @staticmethod
    def _process(path: Path, globals_config: Globals):
        """Process a single file."""
        try:
            file_size = path.stat().st_size
            with MigrationProcessor.total_size_lock:
                MigrationProcessor.total_size += file_size
            
            # Get migration config
            config = MigrationProcessor._get_cfg(path, globals_config)
            
            # Apply migrations
            for migration_func in Migration.functions:
                if (globals_config.start_java_version < migration_func.version <= 
                    globals_config.target_java_version):
                    for func in migration_func.functions:
                        MigrationProcessor._change_content(func, config, globals_config)
                        
        except Exception as e:
            logger.error(f"Error processing file {path}: {e}")
    
    @staticmethod
    def _get_cfg(path: Path, globals_config: Globals) -> MigrationConfig:
        """Get migration configuration for a file."""
        directory = path.parent
        files = {}
        
        try:
            for file_path in directory.iterdir():
                if file_path.is_file():
                    # Read ALL text files in the directory, not just files matching the mask
                    # This is important to get .html files when processing .ts files
                    try:
                        files[file_path.name] = file_path.read_text(encoding='utf-8')
                    except UnicodeDecodeError:
                        # Skip binary files
                        pass
                    except Exception as e:
                        logger.warning(f"Could not read file {file_path}: {e}")
        except Exception as e:
            logger.error(f"Error reading directory {directory}: {e}")
        
        return MigrationConfig(path, files)
    
    @staticmethod
    def _change_content(func: Callable[[MigrationConfig, Globals, str], Content], 
                       config: MigrationConfig, globals_config: Globals):
        """Apply content changes using migration function."""
        try:
            start_time = time.time() * 1000
            content = config.path.read_text(encoding=globals_config.charset)
            new_content = func(config, globals_config, content)
            
            if new_content.changed:
                config.path.write_text(new_content.content, encoding=globals_config.charset)
                end_time = time.time() * 1000
                print(f"\t\tProcessed {config.path} for {int(end_time - start_time)}ms")
                
        except Exception as e:
            logger.error(f"Error with path {config.path}: {e}")


class MetaheuristicMigrationApplication:
    """Main application class."""
    
    def __init__(self, globals_config: Optional[Globals] = None, config_path: Optional[str] = None):
        if globals_config is None:
            if config_path:
                self.globals = Globals.from_yaml(config_path)
            else:
                # Try to find config file
                config_candidates = [
                    "config/application.yml",
                    "application.yml", 
                    "../config/application.yml"
                ]
                
                config_found = False
                for candidate in config_candidates:
                    if Path(candidate).exists():
                        self.globals = Globals.from_yaml(candidate)
                        config_found = True
                        logger.info(f"Using config from {candidate}")
                        break
                
                if not config_found:
                    logger.info("No config file found, using defaults")
                    self.globals = Globals()
        else:
            self.globals = globals_config
            
        self.config = Config(self.globals)
    
    def run(self, *args):
        """Run the migration application."""
        print(f"Started at {time.strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"Configuration:")
        print(f"  Start Java Version: {self.globals.start_java_version}")
        print(f"  Target Java Version: {self.globals.target_java_version}")
        print(f"  File Mask: {self.globals.file_mask}")
        print(f"  Starting Paths: {[str(p) for p in self.globals.starting_path]}")
        print(f"  Exclude Paths: {[str(p) for p in self.globals.exclude_path]}")
        print(f"  Threads: {self.globals.threads}")
        print(f"  Metas: {self.globals.metas}")
        
        try:
            MigrationProcessor.migration_processor(self.globals)
            print(f"Finished at {time.strftime('%Y-%m-%d %H:%M:%S')}")
        except Exception as e:
            logger.error(f"Error during migration: {e}")
            raise
        finally:
            sys.exit(0)
    
    @staticmethod
    def main(args: Optional[List[str]] = None):
        """Main entry point."""
        if args is None:
            args = sys.argv[1:]
        
        config_path = None
        if args and args[0].endswith('.yml'):
            config_path = args[0]
        
        app = MetaheuristicMigrationApplication(config_path=config_path)
        app.run(*args)


if __name__ == "__main__":
    MetaheuristicMigrationApplication.main()
